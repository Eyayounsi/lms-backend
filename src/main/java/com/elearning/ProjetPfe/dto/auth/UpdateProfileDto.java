package com.elearning.ProjetPfe.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la mise à jour du profil utilisateur.
 * L'utilisateur peut modifier : fullName, phone, email + champs étendus.
 * Il ne peut PAS modifier son mot de passe via ce DTO (endpoint séparé).
 */
public class UpdateProfileDto {

    @Size(min = 2, max = 150, message = "Le nom complet doit contenir entre 2 et 150 caractères")
    private String fullName;

    @Email(message = "Format d'email invalide")
    @Size(max = 150)
    private String email;

    @Size(max = 30, message = "Le numéro de téléphone ne doit pas dépasser 30 caractères")
    private String phone;

    // Champs étendus
    private String bio;
    private String aboutMe;
    @Size(max = 150)
    private String designation;
    @Size(max = 300)
    private String address;
    @Size(max = 300)
    private String facebookUrl;
    @Size(max = 300)
    private String instagramUrl;
    @Size(max = 300)
    private String twitterUrl;
    @Size(max = 300)
    private String youtubeUrl;
    @Size(max = 300)
    private String linkedinUrl;
    private String educationJson;
    private String experienceJson;

    // Getters et Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
      // Normalise : vide → null, sinon trim + lowercase pour comparaison fiable
      this.email = (email == null || email.isBlank()) ? null : email.trim().toLowerCase();
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
      this.phone = (phone != null && phone.isBlank()) ? null : phone;
    }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public String getTwitterUrl() { return twitterUrl; }
    public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getEducationJson() { return educationJson; }
    public void setEducationJson(String educationJson) { this.educationJson = educationJson; }

    public String getExperienceJson() { return experienceJson; }
    public void setExperienceJson(String experienceJson) { this.experienceJson = experienceJson; }
}
