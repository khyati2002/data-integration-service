package com.applicate.services.channelkart.masking;

import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.response.OperationResponse;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserMaskingFunction implements MaskingFunction {

    @Override
    public Object mask(Object item) {
        if (item instanceof User) {
            return removeProperties((User) item);
        }
        if (item instanceof OperationResponse) {
            Object feature = ((OperationResponse<?>) item).getFeature();
            if (feature instanceof User) {
                User user = removeProperties((User) feature);
                ((OperationResponse<User>) item).setFeature(user);
                return item;

            }
        }
        return item;
    }

    private List<User> removeProperties(Collection<User> users) {
        return users.stream().map(this::removeProperties).collect(Collectors.toList());
    }

    private User removeProperties(User user) {
        if (user == null) {
            return null;
        }
        user = EntityUtils.deepClone(user);
        user.setMobile(null);
        removeLocationAttributes(user);
        return user;
    }

    private void removeLocationAttributes(User user) {
        JsonNode extendedAttributes = user.getExtendedAttributes();
        if (extendedAttributes instanceof ObjectNode) {
            ((ObjectNode) extendedAttributes).remove("gps_latitude");
            ((ObjectNode) extendedAttributes).remove("gps_longitude");
        }
    }

    @Override
    public String identifier() {
        return "user_mask";
    }
}
