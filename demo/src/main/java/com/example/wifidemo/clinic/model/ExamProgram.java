package com.example.wifidemo.clinic.model;

import java.util.ArrayList;
import java.util.List;

public class ExamProgram {
    private String id;
    private String title;
    private String summary;
    private String description;
    private final List<ExamStep> steps = new ArrayList<>();

    public ExamProgram(String id, String title, String summary, String description) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public List<ExamStep> getSteps() {
        return steps;
    }
}
