package com.elearning.ProjetPfe.dto.auth;

import com.elearning.ProjetPfe.entity.auth.AccountStatus;
import com.elearning.ProjetPfe.entity.auth.Role;
/**
 * DTO en lecture seule pour retourner les données de profil.
 * On ne retourne JAMAIS le mot de passe hashé au frontend.
 */
public class UserProfileDto {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String accountStatus;

    // Champs étendus
    private String avatarPath;
    private String bio;
    private String aboutMe;
    private String designation;
    private String address;
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;
    private String youtubeUrl;
    private String linkedinUrl;
    private String educationJson;
    private String experienceJson;
    private Boolean shareWithRecruiters;
    private Integer challengePoints;

    // Constructeur
    public UserProfileDto() {}

    public UserProfileDto(Long id, String fullName, String email, String phone,
                          String role, String accountStatus) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

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

    public Boolean getShareWithRecruiters() { return shareWithRecruiters; }
    public void setShareWithRecruiters(Boolean shareWithRecruiters) { this.shareWithRecruiters = shareWithRecruiters; }

    public Integer getChallengePoints() { return challengePoints; }
    public void setChallengePoints(Integer challengePoints) { this.challengePoints = challengePoints; }
}
