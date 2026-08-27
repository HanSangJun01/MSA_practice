import logging
from collections import Counter
from typing import List, Optional

from app.client.course_client import course_client
from app.client.enrollment_client import enrollment_client
from app.model.schemas import (
    ElementType,
    MaterialCategory,
    MaterialLotResponse,
    PurchasedLot,
    RecommendedLot,
    RecommendResponse,
)

logger = logging.getLogger(__name__)


class RecommendService:
    """
    규칙 기반 산업 부산물 로트 추천 서비스

    추천 규칙:
    1. 구매기업의 구매 이력에서 카테고리 분석 → 가장 많이 산 카테고리 선택
    2. 해당 카테고리 구매 이력에서 성분 분석 → 가장 많이 산 성분 선택
    3. 해당 카테고리의 APPROVED 로트 조회
    4. 최빈 성분을 많이 포함한 순으로 정렬하여 상위 5개 반환
    5. 구매 이력 없으면 전체 APPROVED 로트 중 계약 건수순 반환
    """

    MAX_RECOMMEND_COUNT = 5  # 최대 추천 로트 수

    async def get_recommendations(self, user_id: int) -> RecommendResponse:
        logger.info(f"[RecommendService] 추천 시작 - userId: {user_id}")

        # 1. 구매 이력 조회 (카테고리 + 성분 포함)
        history = await enrollment_client.get_purchase_history(user_id)
        purchased_lots = history.purchasedLots

        # 2. 구매 이력 없는 신규 구매기업 처리
        if not purchased_lots:
            return await self._recommend_for_new_user(user_id)

        # 3. 가장 많이 산 카테고리 선택
        dominant_category = self._find_dominant_category(purchased_lots)
        if dominant_category is None:
            return await self._recommend_for_new_user(user_id)

        # 4. 가장 많이 산 성분 선택
        dominant_elements = self._find_dominant_elements(purchased_lots, dominant_category)

        # 5. 최빈 카테고리의 APPROVED 로트 조회 (excludeIds 불필요 - 구매 시 SOLD 전환)
        candidates = await course_client.get_recommend_lots(category=dominant_category)

        # 6. 최빈 성분 일치 개수 기준 정렬 후 상위 5개 절단
        recommended = self._rank_by_elements(candidates, dominant_elements)
        recommended = recommended[:self.MAX_RECOMMEND_COUNT]

        logger.info(f"[RecommendService] 추천 완료 - userId: {user_id}, "
                    f"category: {dominant_category.value}, "
                    f"elements: {[e.value for e in dominant_elements]}, "
                    f"count: {len(recommended)}")

        return RecommendResponse(
            userId=user_id,
            recommendedLots=recommended,
            basedOnElements=dominant_elements,
            message=self._build_message(dominant_category, dominant_elements)
        )

    def _find_dominant_category(
        self, purchased_lots: List[PurchasedLot]
    ) -> Optional[MaterialCategory]:
        """
        구매한 로트들의 카테고리 분석 → 최빈 카테고리 반환
        구매 이력 응답에 카테고리가 포함되므로 별도 조회가 필요 없다
        """
        categories = [lot.category for lot in purchased_lots]
        if not categories:
            return None

        # Counter로 최빈 카테고리 선택
        most_common = Counter(categories).most_common(1)
        return most_common[0][0] if most_common else None

    def _find_dominant_elements(
        self,
        purchased_lots: List[PurchasedLot],
        category: MaterialCategory
    ) -> List[ElementType]:
        """
        최빈 카테고리로 구매한 로트들의 성분 분석 → 최빈 성분 반환
        성분은 카테고리에 종속적이므로(PP/PE는 PLASTIC, LITHIUM은 BATTERY)
        다른 카테고리의 성분은 집계에서 제외한다
        동점 성분이 여럿이면 모두 반환한다
        """
        counter = Counter(
            component.name
            for lot in purchased_lots
            if lot.category == category
            for component in lot.components
        )
        if not counter:
            return []

        max_count = counter.most_common(1)[0][1]
        return [element for element, count in counter.items() if count == max_count]

    def _rank_by_elements(
        self,
        candidates: List[MaterialLotResponse],
        dominant_elements: List[ElementType]
    ) -> List[RecommendedLot]:
        """
        후보 로트에 일치 성분을 표시하고 일치 개수 기준 내림차순 정렬
        일치 개수가 같으면 계약 완료 건수가 많은 로트 우선
        """
        target = set(dominant_elements)

        ranked = []
        for lot in candidates:
            matched = [c.name for c in lot.components if c.name in target]
            ranked.append(RecommendedLot(**lot.model_dump(), matchedElements=matched))

        ranked.sort(
            key=lambda lot: (len(lot.matchedElements), lot.enrollmentCount),
            reverse=True
        )
        return ranked

    @staticmethod
    def _build_message(
        category: MaterialCategory,
        elements: List[ElementType]
    ) -> str:
        if not elements:
            return f"{category.value} 카테고리 기반 추천 로트입니다"

        joined = ", ".join(e.value for e in elements)
        return f"{category.value} 카테고리 · {joined} 성분 구매 이력 기반 추천 로트입니다"

    async def _recommend_for_new_user(self, user_id: int) -> RecommendResponse:
        """
        신규 구매기업: 계약 완료 건수 기준 전체 인기 로트 추천
        """
        logger.info(f"[RecommendService] 신규 구매기업 추천 - userId: {user_id}")

        all_lots = await course_client.get_all_lots()
        popular = sorted(
            all_lots,
            key=lambda lot: lot.enrollmentCount,
            reverse=True
        )[:self.MAX_RECOMMEND_COUNT]

        return RecommendResponse(
            userId=user_id,
            recommendedLots=[RecommendedLot(**lot.model_dump()) for lot in popular],
            basedOnElements=[],
            message="거래가 많은 인기 로트 추천입니다"
        )


recommend_service = RecommendService()
