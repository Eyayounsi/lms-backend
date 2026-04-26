package com.elearning.ProjetPfe.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyRegisterOtpDto {

    @NotBlank(message = "Email requis")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Code OTP requis")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code OTP doit contenir exactement 6 chiffres")
    private String otpCode;

    public VerifyRegisterOtpDto() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
