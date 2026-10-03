package org.ruoyi.service.shortdrama.impl;

/** Only controlled, user-actionable voice validation messages may be displayed by the controller. */
public final class ShortDramaVoiceException extends IllegalArgumentException {
    public ShortDramaVoiceException(String message) {super(message);}
    public ShortDramaVoiceException(String message,Throwable cause) {super(message,cause);}
}
