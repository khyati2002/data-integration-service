package com.applicate.services.channelkart.commandline;

import com.applicate.services.channelkart.models.Profile;
import com.applicate.services.channelkart.profiles.ProfileRegistry;
import com.applicate.services.channelkart.services.SpringContext;

import java.util.stream.Collectors;

public class Profiles {

    public static Profile get(String lob, String name){
        if(SpringContext.isContextInitialized()){
            return ProfileRegistry.INSTANCE.get(lob).stream().filter(s->s.getName().equalsIgnoreCase(name)).collect(
                    Collectors.toList()).get(0);
        }else {
            throw new UnsupportedOperationException("Profiles not initialized");
        }
    }

}
