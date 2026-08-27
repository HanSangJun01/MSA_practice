import batteryImg from '@/assets/images/courses/battery.png'
import chemicalImg from '@/assets/images/courses/chemical.png'
import constructionImg from '@/assets/images/courses/construction.png'
import electronicImg from '@/assets/images/courses/electronic.png'
import metalImg from '@/assets/images/courses/metal.png'
import plasticImg from '@/assets/images/courses/plastic.png'
import textileImg from '@/assets/images/courses/textile.png'
import { getCategoryCode } from './category.js'

// OTHER 카테고리는 매칭되는 사진이 없어 매핑에서 제외한다 (호출부에서 사진 영역을 생략)
export const CATEGORY_IMAGES = {
  METAL: metalImg,
  PLASTIC: plasticImg,
  BATTERY: batteryImg,
  ELECTRONIC: electronicImg,
  CHEMICAL: chemicalImg,
  CONSTRUCTION: constructionImg,
  TEXTILE: textileImg,
}

export function getCategoryImage(raw) {
  return CATEGORY_IMAGES[getCategoryCode(raw)] || null
}
