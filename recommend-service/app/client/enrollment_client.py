import httpx
import logging
from app.config.settings import settings
from app.model.schemas import PurchaseHistoryResponse

logger = logging.getLogger(__name__)


class EnrollmentServiceClient:
    """
    Enrollment Service REST 클라이언트
    - 구매 이력 조회 (구매한 로트의 카테고리 + 성분)
    """

    def __init__(self):
        self.base_url = settings.enrollment_service_url

    async def get_purchase_history(self, user_id: int) -> PurchaseHistoryResponse:
        """
        GET /enrollments/internal/history/{userId}
        구매기업이 구매한 로트 목록 조회 (카테고리·성분 포함)
        """
        url = f"{self.base_url}/api/enrollments/internal/history/{user_id}"
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(url)
                response.raise_for_status()
                data = response.json()
                return PurchaseHistoryResponse(**data)
        except Exception as e:
            logger.error(f"[EnrollmentClient] 구매 이력 조회 실패 - userId: {user_id}, error: {e}")
            # 실패 시 빈 이력 반환 (추천 서비스는 비핵심 기능)
            return PurchaseHistoryResponse(userId=user_id, activeCourseIds=[], purchasedLots=[])


enrollment_client = EnrollmentServiceClient()
