import httpx
import logging
from typing import List, Optional
from app.config.settings import settings
from app.model.schemas import MaterialLotResponse, MaterialCategory

logger = logging.getLogger(__name__)


class CourseServiceClient:
    """
    Course Service REST 클라이언트
    - 카테고리별 판매 가능(APPROVED) 로트 목록 조회
    """

    def __init__(self):
        self.base_url = settings.course_service_url

    async def get_recommend_lots(
        self,
        category: Optional[MaterialCategory] = None
    ) -> List[MaterialLotResponse]:
        """
        GET /courses/internal/recommend
        카테고리 기반 APPROVED 로트 목록 조회 (category 생략 시 APPROVED 전체)

        구매된 로트는 즉시 SOLD로 전환되어 애초에 후보에 없으므로
        excludeIds 파라미터는 사용하지 않는다.

        외부 GET /api/courses 는 같은 로트를 supplierId/contractCount 라는
        다른 필드명으로 내려주므로 추천 서비스에서는 사용하지 않는다.
        """
        url = f"{self.base_url}/api/courses/internal/recommend"
        params = {}
        if category is not None:
            params["category"] = category.value

        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
                return [MaterialLotResponse(**lot) for lot in response.json()]
        except Exception as e:
            logger.error(f"[CourseClient] 추천 로트 조회 실패 - category: {category}, error: {e}")
            return []


course_client = CourseServiceClient()
