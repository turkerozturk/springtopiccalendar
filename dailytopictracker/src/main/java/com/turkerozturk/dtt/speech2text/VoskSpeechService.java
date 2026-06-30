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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoskSpeechService {

    private static final String DICTATION_MODEL_PATH = "C:\\PROJELERIM\\SpringTopicCalendarGithub\\voskmodels\\tr";

    private static final float SAMPLE_RATE = 48000f;

    private static final String CHARSET_NAME = "windows-1254";

    private final ObjectMapper mapper = new ObjectMapper();

    private TargetDataLine microphone;

    private Model modelDictation;

    private Recognizer recognizer;

    private Thread recognitionThread;

    private volatile boolean initialized = false;




    private final ConcurrentHashMap<
            String,
            SpeechSession> sessions =
            new ConcurrentHashMap<>();

    private volatile SpeechSession activeSession;

    private volatile boolean recording = false;

    public void register(WebSocketSession session) {

        SpeechSession speechSession =
                new SpeechSession(session);

        sessions.put(
                session.getId(),
                speechSession);

        activeSession = speechSession;

        System.out.println(
                "Client connected : "
                        + session.getId());

        if (!initialized) {

            initializeRecognition();

            initialized = true;
        }
    }


    public synchronized void unregister(
            WebSocketSession session){

        SpeechSession removed =
                sessions.remove(
                        session.getId());

        if(removed==activeSession){

            activeSession=null;

        }

        System.out.println(
                "Client disconnected : "
                        + session.getId());
    }

    private void initializeRecognition() {

        try {

            modelDictation =
                    new Model(
                            DICTATION_MODEL_PATH);

            AudioFormat format =
                    new AudioFormat(
                            SAMPLE_RATE,
                            16,
                            1,
                            true,
                            false);

            DataLine.Info info =
                    new DataLine.Info(
                            TargetDataLine.class,
                            format);

            microphone =
                    (TargetDataLine)
                            AudioSystem.getLine(info);

            microphone.open(format);

            microphone.start();

            recognizer =
                    new Recognizer(
                            modelDictation,
                            SAMPLE_RATE);

            recognitionThread =
                    new Thread(this::recognitionLoop);

            recognitionThread.setDaemon(true);

            recognitionThread.start();

        }
        catch (Exception ex) {

            throw new RuntimeException(ex);
        }

    }

    private void recognitionLoop() {

        byte[] buffer =
                new byte[4096];

        while (true) {

            try {

                int bytesRead =
                        microphone.read(
                                buffer,
                                0,
                                buffer.length);

              //  System.out.println("loop");

                if (bytesRead <= 0) {
                    continue;
                }

                /* TODO gecici olarak commen ettik
                if (!recording) {
                    continue;
                }
                */

                boolean completed =
                        recognizer.acceptWaveForm(
                                buffer,
                                bytesRead);

            //    System.out.println("is completed: " + completed);

                if (completed) {

                    String json =
                            recognizer.getResult();

                    String text =
                            extractResult(json);

                    if (!text.isBlank()) {

                        if(activeSession!=null){


                            // metnin ilk harfini buyuk yapalim, sonuna nokta ve bosluk koyalim.
                            text = text.substring(0, 1).toUpperCase(new Locale("tr", "TR")) + text.substring(1);
                            text += ".";

                            activeSession
                                    .getFinalText()
                                    .append(text)
                                    .append(" ");

                        }


                        broadcastFinal(text);

                    }

                }
                else {

                    String json =
                            recognizer.getPartialResult();

            //        System.out.println(json);

                    String partial =
                            extractPartial(json);

             //       System.out.println(partial);

                    if (!partial.isBlank()) {

                        if(activeSession!=null){

                            activeSession
                                    .setPartialText(
                                            partial);

                        }

                        broadcastPartial(partial);

                    }

                }





            }
            catch (Exception ex) {

                ex.printStackTrace();
            }

        }

    }

    private String extractPartial(String json) {

        try {

            JsonNode node =
                    mapper.readTree(json);

            String text =
                    node.path("partial")
                            .asText("");

            return fixTurkish(text);

        } catch (Exception ex) {

            return "";
        }
    }

    private String fixTurkish(String text) {

        try {

            return new String(
                    text.getBytes(CHARSET_NAME));

        } catch (Exception ex) {

            return text;
        }
    }

    private void broadcastPartial(String text) {

        sendMessage(
                new SpeechMessage(
                        "partial",
                        text));

    }




    private String extractResult(String json) {

        try {

            JsonNode node =
                    mapper.readTree(json);

            return fixTurkish(
                    node.path("text")
                            .asText(""));

        }
        catch (Exception ex) {

            return "";

        }

    }

    private void broadcastFinal(String text) {

        SpeechMessage message =
                new SpeechMessage(
                        "final",
                        text);

        sendMessage(message);

    }


    private void sendMessage(
            SpeechMessage message){

        if(activeSession==null){

            return;

        }

        try{

            String json=
                    mapper.writeValueAsString(
                            message);

            if(activeSession
                    .getSocket()
                    .isOpen()){

                activeSession
                        .getSocket()
                        .sendMessage(
                                new TextMessage(json));

            }

        }
        catch(Exception ex){

            ex.printStackTrace();

        }

    }

    public void startRecording() {

        recording = true;

        recognizer.reset();

        System.out.println("Recording started");
    }

    public void stopRecording() {

        recording = false;

        System.out.println("Recording stopped");
    }


}