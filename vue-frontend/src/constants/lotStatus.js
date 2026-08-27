// course-service Course.Status와 1:1로 맞춘 판매 로트 상태
export const LOT_STATUS = {
  PENDING: { label: '검토중', badge: 'badge-amber' },
  APPROVED: { label: '승인됨', badge: 'badge-accent' },
  REJECTED: { label: '거절됨', badge: 'badge-error' },
  RESERVED: { label: '예약됨', badge: 'badge-gray' },
  SOLD: { label: '판매완료', badge: 'badge-accent' },
  WITHDRAWN: { label: '철회됨', badge: 'badge-gray' },
}

export function getLotStatusLabel(raw) {
  return LOT_STATUS[raw]?.label || raw || '-'
}

export function getLotStatusBadge(raw) {
  return LOT_STATUS[raw]?.badge || 'badge-gray'
}
