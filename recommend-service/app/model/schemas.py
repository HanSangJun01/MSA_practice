from pydantic import BaseModel
from typing import List, Optional
from enum import Enum
from decimal import Decimal
from datetime import datetime


class MaterialCategory(str, Enum):
    """산업 부산물 카테고리"""
    METAL = "METAL"
    PLASTIC = "PLASTIC"
    BATTERY = "BATTERY"
    ELECTRONIC = "ELECTRONIC"
    CHEMICAL = "CHEMICAL"
    CONSTRUCTION = "CONSTRUCTION"
    TEXTILE = "TEXTILE"
    OTHER = "OTHER"


class ElementType(str, Enum):
    """로트에 포함된 성분"""
    LITHIUM = "LITHIUM"
    COBALT = "COBALT"
    NICKEL = "NICKEL"
    COPPER = "COPPER"
    ALUMINUM = "ALUMINUM"
    IRON = "IRON"
    MANGANESE = "MANGANESE"
    PET = "PET"
    PP = "PP"
    PE = "PE"
    PVC = "PVC"
    OTHER = "OTHER"


class MaterialComponent(BaseModel):
    """성분 1건 (성분명 + 함량 %)"""
    name: ElementType
    percentage: Optional[Decimal] = None


class MaterialLotResponse(BaseModel):
    """Course Service가 반환하는 판매 로트 (APPROVED)"""
    id: int
    title: str
    description: Optional[str] = None
    category: MaterialCategory
    components: List[MaterialComponent] = []
    region: Optional[str] = None
    quantity: Optional[Decimal] = None
    price: Optional[Decimal] = None
    instructorId: Optional[int] = None
    enrollmentCount: int = 0
    status: Optional[str] = None
    createdAt: Optional[datetime] = None


class RecommendedLot(MaterialLotResponse):
    """추천 로트 = 로트 정보 + 최빈 성분과 일치한 성분 목록"""
    matchedElements: List[ElementType] = []


class PurchasedLot(BaseModel):
    """구매기업이 이미 구매한 로트"""
    courseId: int
    category: MaterialCategory
    components: List[MaterialComponent] = []


class PurchaseHistoryResponse(BaseModel):
    """GET /api/enrollments/internal/history/{userId} 응답"""
    userId: int
    activeCourseIds: List[int] = []
    purchasedLots: List[PurchasedLot] = []


class RecommendResponse(BaseModel):
    """GET /api/recommend/{userId} 응답"""
    userId: int
    recommendedLots: List[RecommendedLot]
    basedOnElements: List[ElementType] = []
    message: str


class ApiResponse(BaseModel):
    success: bool
    message: str
    data: Optional[dict] = None
