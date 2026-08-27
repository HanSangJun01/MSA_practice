package com.lecture.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.math.BigDecimal;

/**
 * 판매 로트의 성분·함량 한 건
 *
 * 성분명을 자유 문자열로 두면 "리튬" / "LITHIUM" / "Li" 가 섞여
 * 추천 서비스의 성분 교집합이 잡히지 않으므로 반드시 enum 으로 강제한다.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class MaterialComponent {

    @Enumerated(EnumType.STRING)
    @Column(name = "component_name", nullable = false, length = 30)
    private ComponentName name;

    /** 함량 0~100 */
    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    public enum ComponentName {
        LITHIUM, COBALT, NICKEL, COPPER, ALUMINUM, IRON, MANGANESE,
        PET, PP, PE, PVC, OTHER
    }
}
