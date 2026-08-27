// course-service Course.Category와 1:1로 맞춘 산업 부산물 카테고리
export const CATEGORY_LABELS = {
  METAL: '금속',
  PLASTIC: '플라스틱',
  BATTERY: '배터리',
  ELECTRONIC: '전자폐기물',
  CHEMICAL: '화학물질',
  CONSTRUCTION: '건설폐기물',
  TEXTILE: '섬유',
  OTHER: '기타',
}

export const CATEGORY_OPTIONS = Object.entries(CATEGORY_LABELS).map(([value, label]) => ({ value, label }))

// 백엔드가 영문 코드(METAL 등)를 그대로 내려주는 경우와, 이미 한글로 변환돼 온 경우를 모두 처리
export function getCategoryLabel(raw) {
  if (!raw) return ''
  return CATEGORY_LABELS[raw] || raw
}

// 표시용 문자열이든 원본 코드든 받아서 아이콘 매칭에 쓸 표준 코드로 되돌린다
export function getCategoryCode(raw) {
  if (!raw) return ''
  if (CATEGORY_LABELS[raw]) return raw
  const found = Object.entries(CATEGORY_LABELS).find(([, label]) => label === raw)
  return found ? found[0] : raw
}
