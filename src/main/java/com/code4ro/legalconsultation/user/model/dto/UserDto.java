package com.code4ro.legalconsultation.user.model.dto;

import com.code4ro.legalconsultation.core.model.dto.BaseEntityDto;
import com.code4ro.legalconsultation.core.model.dto.validator.UniqueUserEmailConstraint;
import com.code4ro.legalconsultation.user.model.persistence.UserRole;
import com.code4ro.legalconsultation.user.model.persistence.UserSpecialization;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class UserDto extends BaseEntityDto {
    @Size(max = 40, message = "user.save.firstName.tooLong")
    private String firstName;
    @Size(max = 40, message = "user.save.lastName.tooLong")
    private String lastName;
    @Size(max = 40, message = "user.save.phoneNumber.tooLong")
    private String phoneNumber;
    @NotBlank
    @Size(max = 40)
    @Email
    @UniqueUserEmailConstraint
    private String email;
    @Size(max = 40, message = "user.save.district.tooLong")
    private String district;
    @Size(max = 100, message = "user.save.organisation.tooLong")
    private String organisation;
    // added default value to prevent breaking when adding a user from the frontend,
    // where the specialization can't yet be selected
    @NotNull(message = "user.save.specialization.null")
    private UserSpecialization specialization = UserSpecialization.ARCHITECT;
    @NotNull(message = "user.save.role.null")
    private UserRole role;
}
