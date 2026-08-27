// course-service MaterialComponent.ComponentName과 1:1로 맞춘 성분명
export const COMPONENT_LABELS = {
  LITHIUM: '리튬',
  COBALT: '코발트',
  NICKEL: '니켈',
  COPPER: '구리',
  ALUMINUM: '알루미늄',
  IRON: '철',
  MANGANESE: '망간',
  PET: 'PET',
  PP: 'PP',
  PE: 'PE',
  PVC: 'PVC',
  OTHER: '기타',
}

export const COMPONENT_OPTIONS = Object.entries(COMPONENT_LABELS).map(([value, label]) => ({ value, label }))

export function getComponentLabel(raw) {
  if (!raw) return ''
  return COMPONENT_LABELS[raw] || raw
}
