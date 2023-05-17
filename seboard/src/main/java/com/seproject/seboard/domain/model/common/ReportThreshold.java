package com.seproject.seboard.domain.model.common;

import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Table(name = "report_thresholds")
public class ReportThreshold {
    @Id @GeneratedValue
    private Long thresholdId;
    private int threshold;
    @Enumerated(EnumType.STRING)
    private ReportType thresholdType;

    public static ReportThreshold of(int threshold, String thresholdType){
        return new ReportThreshold(threshold, thresholdType);
    }

    private ReportThreshold(int threshold, String thresholdType){
        this.threshold = threshold;
        this.thresholdType = ReportType.valueOf(thresholdType);

    }

    public boolean isOverThreshold(int reportCount){
        return reportCount >= threshold;
    }
}
