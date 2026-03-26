package com.example.wifidemo.clinic.model;

public class PatientProfile {
    private String id;
    private String name;
    private String phone;
    private String gender;
    private String birthDate;
    private String address;
    private String note;

    public PatientProfile() {
    }

    public PatientProfile(
            String id,
            String name,
            String phone,
            String gender,
            String birthDate,
            String address,
            String note
    ) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.note = note;
    }

    public PatientProfile copy() {
        return new PatientProfile(id, name, phone, gender, birthDate, address, note);
    }

    public String getDisplayName() {
        return name == null || name.trim().isEmpty() ? "未命名被测者" : name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
