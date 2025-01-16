package com.applicate.services.channelkart.exceptions;

import java.io.Serializable;

public interface ErrorCode extends Serializable {

    String getCode();

    String getReason();

}
