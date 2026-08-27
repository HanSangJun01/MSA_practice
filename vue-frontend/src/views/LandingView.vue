<template>
  <div class="landing">
    <AppHeader />

    <!-- 히어로 섹션 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-content fade-in-up">
          <span class="hero-badge">성분 기반 순환거래 플랫폼</span>
          <h1 class="hero-title">부산물을 보지 마세요, <br>성분을 보세요</h1>
          <p class="hero-desc"> 공장과 공장을 잇는 가장 스마트한 원소 매칭 시스템 </p>
          <div class="hero-actions">
            <router-link to="/login" class="btn btn-accent-green btn-lg">Elemento Logging</router-link>
            <router-link to="/courses" class="btn btn-on-dark-outline btn-lg">Elemento Match</router-link>
          </div>
          <div class="hero-stats">
            <div class="stat"><span class="stat-num">1,200+</span><span class="stat-label">등록 원료</span></div>
            <div class="stat"><span class="stat-num">340+</span><span class="stat-label">공급기업</span></div>
            <div class="stat"><span class="stat-num">28,000+</span><span class="stat-label">계약 건수</span></div>
          </div>
        </div>
        <div class="hero-visual fade-in">
          <img src="@/assets/images/logo/elemento_logo_transparent.png" alt="LearnNexus" class="hero-logo" />
        </div>
      </div>
    </section>

    <!-- 인기 원료 -->
    <section class="popular-section">
      <div class="section-inner">
        <div class="section-header">
          <h2 class="section-title">인기 원료</h2>
          <router-link to="/login" class="section-link">전체 보기 →</router-link>
        </div>
        <div class="course-grid">
          <div v-for="course in featuredCourses" :key="course.id" class="course-card-landing">
            <div class="card-thumb" :class="course.thumbBg">
              <img :src="course.thumbSrc" :alt="course.title" class="thumb-img" />
            </div>
            <div class="card-body">
              <span class="badge" :class="course.badgeClass">{{ course.category }}</span>
              <h3 class="card-title">{{ course.title }}</h3>
              <div class="card-meta">
                <span class="instructor">{{ course.supplier }}</span>
                <span class="price">{{ course.price }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 특징 섹션 -->
    <section class="features-section">
      <div class="section-inner">
        <h2 class="section-title center">왜 Elemento인가요?</h2>
        <div class="features-grid">
          <div v-for="f in features" :key="f.title" class="feature-card">
            <div class="feature-icon">{{ f.icon }}</div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-section">
      <div class="cta-inner">
        <h2>지금 바로 시작하세요</h2>
        <p>수백 개 기업이 Elemento와 함께 부산물을 자원으로 바꾸고 있습니다.</p>
        <router-link to="/login" class="btn btn-primary btn-lg">무료로 시작하기</router-link>
      </div>
    </section>

    <!-- 푸터 -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-logo">
          <img src="@/assets/images/logo/elemento_logo.png" alt="LearnNexus" />
          <span>LearnNexus</span>
        </div>
        <p class="footer-copy">© 2026 Elemento. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'

import springImg   from '@/assets/images/courses/spring_boot.png'
import vueImg      from '@/assets/images/courses/vue_js.png'
import k8sImg      from '@/assets/images/courses/kubernetes.png'
import dockerImg   from '@/assets/images/courses/docker.png'
import pythonImg   from '@/assets/images/courses/python.png'
import genaiImg    from '@/assets/images/courses/generative_ai.png'

const featuredCourses = [
  { id:1, title:'리튬이온 배터리 블랙매스', category:'폐건전지',   supplier:'리커버메탈',   price:'₩1,200,000', thumbSrc: k8sImg,    thumbBg:'thumb-blue',   badgeClass:'badge-blue'   },
  { id:2, title:'반도체 폐수슬러지 (CaF₂)', category:'폐수슬러지', supplier:'케미리사이클', price:'협의',        thumbSrc: dockerImg, thumbBg:'thumb-gray',   badgeClass:'badge-gray'   },
  { id:3, title:'제철소 고로 슬래그',       category:'제철슬래그', supplier:'스틸사이클',   price:'협의',        thumbSrc: springImg, thumbBg:'thumb-orange', badgeClass:'badge-orange' },
  { id:4, title:'폐합성수지 열분해유 원료', category:'폐합성수지', supplier:'리사이클텍',   price:'₩620,000',   thumbSrc: pythonImg, thumbBg:'thumb-amber',  badgeClass:'badge-amber'  },
  { id:5, title:'구리 스크랩·동선',         category:'스크랩금속', supplier:'메탈리커버리', price:'₩1,050,000', thumbSrc: vueImg,    thumbBg:'thumb-blue',   badgeClass:'badge-blue'   },
  { id:6, title:'수산 가공 부산물 (콜라겐 원료)', category:'식품부산물', supplier:'크래프트바이오', price:'₩180,000', thumbSrc: genaiImg,  thumbBg:'thumb-green',  badgeClass:'badge-green'  },
]

const features = [
  { icon:'🧪', title:'성분 기반 AI 매칭', desc:'필요 성분을 등록하면 부산물 성분표와 대조해 최적의 거래를 먼저 제안합니다 (Elemento Match).' },
  { icon:'✅', title:'제3자 품질 검증', desc:'공인 시험기관이 검증한 로트별 성분 분석표로 안심하고 거래하세요.' },
  { icon:'💳', title:'간편한 계약 결제', desc:'원클릭 계약과 즉시 결제로 거래를 빠르게 진행하세요.' },
  { icon:'📊', title:'ESG·비용 절감 리포트', desc:'폐기비용 절감액과 탄소 절감량을 한눈에 확인하세요.' },
]
</script>

<style scoped>
.landing { background: var(--color-bg-secondary); }

/* 히어로 — 다크 틸 → 민트 atmospheric 그라데이션 */
.hero {
  background: linear-gradient(160deg, #06120F 0%, #0B2A24 42%, #0F6E56 100%);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  padding: 88px 0 72px;
}
.hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 48px;
  align-items: center;
}
.hero-badge {
  display: inline-block;
  padding: 5px 14px;
  background: rgba(0, 212, 164, 0.16);
  color: var(--color-brand-green);
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 16px;
}
.hero-title {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.5px;
  color: var(--color-on-dark);
  margin-bottom: 16px;
}
.hero-desc {
  font-size: 16px;
  color: var(--color-on-dark-muted);
  line-height: 1.7;
  max-width: 460px;
  margin-bottom: 28px;
}
.hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 40px;
}
.btn-lg { padding: 12px 28px; font-size: 15px; }
.btn-accent-green {
  background: var(--color-brand-green);
  color: #06120F;
  border: 1.5px solid var(--color-brand-green);
  font-weight: 600;
}
.btn-accent-green:hover {
  background: #33DDB4;
  border-color: #33DDB4;
  box-shadow: var(--shadow-accent);
}
.btn-on-dark-outline {
  background: transparent;
  color: var(--color-on-dark);
  border: 1.5px solid rgba(255,255,255,0.28);
}
.btn-on-dark-outline:hover {
  background: rgba(255,255,255,0.08);
  border-color: rgba(255,255,255,0.4);
}
.hero-stats {
  display: flex;
  gap: 36px;
}
.stat { display: flex; flex-direction: column; gap: 2px; }
.stat-num { font-size: 22px; font-weight: 700; color: var(--color-brand-green); }
.stat-label { font-size: 12px; color: var(--color-on-dark-muted); }
.hero-visual {
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-logo {
  width: 560px;
  height: 560px;
  object-fit: contain;
}

/* 강의 섹션 */
.popular-section { padding: 64px 0; }
.section-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; }
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.section-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.section-title.center { text-align: center; margin-bottom: 40px; }
.section-link { font-size: 14px; color: var(--color-primary); font-weight: 500; }
.section-link:hover { text-decoration: underline; }

.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.course-card-landing {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: var(--transition);
}
.course-card-landing:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.card-thumb {
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.thumb-green  { background: var(--color-brand-green-soft); }
.thumb-blue   { background: rgba(55, 114, 207, 0.12); }
.thumb-orange { background: rgba(242, 104, 60, 0.12); }
.thumb-amber  { background: rgba(217, 119, 6, 0.12); }
.thumb-gray   { background: var(--color-bg-tertiary); }
.thumb-img { width: 100%; height: 100%; object-fit: contain; padding: 14px; }
.card-body { padding: 14px 16px; display: flex; flex-direction: column; gap: 6px; }
.card-title { font-size: 14px; font-weight: 600; color: var(--color-text-primary); line-height: 1.4; }
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.instructor { font-size: 12px; color: var(--color-text-secondary); }
.price { font-size: 14px; font-weight: 600; color: var(--color-primary); }

/* 특징 */
.features-section { padding: 64px 0; background: var(--color-bg-primary); }
.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.feature-card {
  padding: 28px 24px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  text-align: center;
  transition: var(--transition);
}
.feature-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.feature-icon { font-size: 32px; margin-bottom: 12px; }
.feature-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.feature-desc { font-size: 13px; color: var(--color-text-secondary); line-height: 1.6; }

/* CTA — promo-banner 패턴 (canvas-dark 배경 + on-dark 텍스트 + on-dark 화이트 필 버튼) */
.cta-section {
  padding: 80px 0;
  background: var(--color-canvas-dark);
  text-align: center;
}
.cta-inner { max-width: 600px; margin: 0 auto; padding: 0 24px; }
.cta-inner h2 { font-size: 32px; font-weight: 700; color: var(--color-on-dark); margin-bottom: 12px; }
.cta-inner p { font-size: 16px; color: var(--color-on-dark-muted); margin-bottom: 32px; }
.cta-inner .btn-primary {
  background: var(--color-on-dark);
  color: var(--color-primary);
  border-color: var(--color-on-dark);
  font-weight: 600;
}
.cta-inner .btn-primary:hover { background: #fff; }

/* 푸터 — design.md footer-region: canvas 화이트 + 상단 hairline */
.footer {
  background: var(--color-bg-primary);
  border-top: 1px solid var(--color-border);
  padding: 32px 0;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 600;
}
.footer-logo img { width: 40px; height: 40px; border-radius: 6px; }
.footer-copy { font-size: 13px; color: var(--color-text-muted); }
</style>
