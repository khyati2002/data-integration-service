package com.applicate.services.channelkart.validations.repository;

import com.applicate.services.channelkart.models.enums.UserMetadataType;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.jooq.impl.UserMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Checks if a MOBILE_NUMBER type metadata's value is already present
 * with a *different* user in the db.
 */
public class UserMetadataMobileValidator extends AbstractValidationRule<UserMetadata> {

    @Override
    public OperationResult.StepResult apply(UserMetadata cdm) {
        final UserService userService = (UserService) ServiceLocator.lookup(User.class);

        if (cdm.getType().equals(UserMetadataType.MOBILE_NUMBER.name())) {
            Optional<List<User>> usersInDB = userService.findByMobileSafely(cdm.getValue());

            if (usersInDB.isPresent()) {
                boolean foundOtherUser = usersInDB.get().stream()
                        .anyMatch(user -> !user.getLoginid().equalsIgnoreCase(cdm.getLoginid()));

                if (foundOtherUser) {
                    return new OperationResult.StepResult(OperationResult.Status.ERROR, "Mobile number already present with an existing user");
                }
            }
        }
        return OperationResult.StepResult.OK;
    }
}