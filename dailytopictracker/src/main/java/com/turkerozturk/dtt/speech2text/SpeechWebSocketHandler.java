/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package com.turkerozturk.dtt.speech2text;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class SpeechWebSocketHandler
        extends TextWebSocketHandler {

    private final VoskSpeechService speechService;

    private final ObjectMapper mapper =
            new ObjectMapper();

    public SpeechWebSocketHandler(
            VoskSpeechService speechService) {

        this.speechService = speechService;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session)
            throws Exception {

        speechService.register(session);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status)
            throws Exception {

        speechService.unregister(session);
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message)
            throws Exception {

        SpeechControlMessage control =
                mapper.readValue(
                        message.getPayload(),
                        SpeechControlMessage.class);

        switch (control.getAction()) {

            case "start":
                speechService.startRecording();
                break;

            case "stop":
                speechService.stopRecording();
                break;

        }

    }

}