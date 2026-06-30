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

let finalText="";

let socket = null;

document
    .getElementById("connect")
    .onclick = function () {

        socket =
            new WebSocket(
                "ws://localhost:8080/ws/speech");

        socket.onopen = function () {

            console.log("CONNECTED");
        };

        socket.onclose = function () {

            console.log("CLOSED");
        };

        socket.onmessage = function (e) {
            //    document.getElementById("note").value = e.data;

           // console.log(e.data);

           const msg = JSON.parse(e.data);




           const mtype = msg.type;

            if(mtype == "final") {
                document.getElementById("note").value = msg.text;
            } else {
              //  document.getElementById("note").value = "pariyal";

            }



        };


        document
            .getElementById("start")
            .onclick=function(){

            socket.send(
                JSON.stringify({
                    action:"start"
                }));

        };

        document
            .getElementById("stop")
            .onclick=function(){

            socket.send(
                JSON.stringify({
                    action:"stop"
                }));

        };


    };