package com.licenta.chatin.models;

import com.licenta.chatin.utilities.Constants;

import java.util.Date;

public class ChatMessage {

    public String senderId;
    public String receiverId;
    public String message;
    public String dateTime;
    public Date dateObject;
    public String conversionId;
    public String conversionName;
    public String conversionImage;

    public boolean isImage() {
        return message != null && message.startsWith(Constants.MESSAGE_IMAGE_PREFIX);
    }
}
