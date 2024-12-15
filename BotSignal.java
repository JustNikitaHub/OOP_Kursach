package com.kursach;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;

public class BotSignal extends TelegramLongPollingBot {


    private static final String BOT_TOKEN = "";
    private static final String BOT_USERNAME = "";

    public static String toWeekString(String a){
        Map<String,String> strNum = new HashMap<String,String>();
        strNum.put("MON", "0");
        strNum.put("TUE", "1");
        strNum.put("WED", "2");
        strNum.put("THU", "3");
        strNum.put("FRI", "4");
        strNum.put("SAT", "5");
        return strNum.get(a.toUpperCase());
    }
    public static String toStringWeek(String a){
        Map<String,String> strNum = new HashMap<String,String>();
        strNum.put("0", "MON");
        strNum.put( "1","TUE" );
        strNum.put( "2","WED" );
        strNum.put( "3","THU" );
        strNum.put( "4","FRI" );
        strNum.put( "5","SAT" );
        return strNum.get(a.toUpperCase());
    }

    public static String getSeason(){
        LocalDate today = LocalDate.now();
        Month currentMonth = today.getMonth();
        String season = (currentMonth.getValue() >= Month.SEPTEMBER.getValue() && currentMonth.getValue() <= Month.DECEMBER.getValue())
                        ? "autumn"
                        : "spring";
        return season;
    }

    public static String getDaySchedule(String day, int weekNumber, String groupNumber) throws Exception {
        String apiUrl = "https://digital.etu.ru/api/mobile/schedule?weekDay=" + day.toUpperCase() + "&groupNumber=" + groupNumber + "&joinWeeks=false&season="+getSeason()+"&year="+LocalDate.now().getYear();
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Charset", "UTF-8");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject groupSchedule = jsonResponse.getJSONObject(groupNumber);
        JSONObject daysSchedule = groupSchedule.getJSONObject("days");
        JSONObject selectedDay = daysSchedule.optJSONObject(toWeekString(day));
        if (selectedDay == null) {
            return "Нет занятий на этот день.";
        }
        JSONArray lessons = selectedDay.getJSONArray("lessons");
        StringBuilder result = new StringBuilder("Расписание на " + day + ":\n");
        boolean found = false;
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.getJSONObject(i);
            String start_time = lesson.getString("start_time");
            String end_time = lesson.getString("end_time");
            String teacher = lesson.getString("teacher");
            String name = lesson.getString("name");
            String room = lesson.getString("room");
            String subjectType = lesson.getString("subjectType");
            int week = lesson.getInt("week");

            if (week == 0 || week == weekNumber) {
                found = true;
                result.append("("+subjectType+")").append(name).append(" - ").append(teacher).append("\n").append(start_time).append(" - ").append(end_time).append("\n").append("Аудитория: "+room+"\n").append("\n");
            }
        }

        if (!found) {
            result.append("Занятий в этот день на выбранной неделе нет.\n");
        }

        return result.toString();
    }

    
    public static String getAllSchedule(int weekNumber, String groupNumber) throws Exception {
        String apiUrl = "https://digital.etu.ru/api/mobile/schedule?groupNumber=" + groupNumber + "&joinWeeks=false&season="+getSeason()+"&year="+LocalDate.now().getYear();
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Charset", "UTF-8");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject groupSchedule = jsonResponse.getJSONObject(groupNumber);
        JSONObject daysSchedule = groupSchedule.getJSONObject("days");
        StringBuilder result = new StringBuilder();
        for(int d=0; d<6; d++)
        {
            String day = Integer.toString(d);
            JSONObject selectedDay = daysSchedule.optJSONObject(day);
            if (selectedDay == null) {
                return "Нет занятий на этот день.";
            }
            JSONArray lessons = selectedDay.getJSONArray("lessons");
            result.append("Расписание на "+toStringWeek(day)+":\n");
            boolean found = false;
            for (int i = 0; i < lessons.length(); i++) {
                JSONObject lesson = lessons.getJSONObject(i);
                String start_time = lesson.getString("start_time");
                String end_time = lesson.getString("end_time");
                String teacher = lesson.getString("teacher");
                String name = lesson.getString("name");
                String room = lesson.getString("room");
                String subjectType = lesson.getString("subjectType");
                int week = lesson.getInt("week");

                if (week == 0 || week == weekNumber) {
                    found = true;
                    result.append("("+subjectType+")").append(name).append(" - ").append(teacher).append("\n").append(start_time).append(" - ").append(end_time).append("\n").append("Аудитория: "+room+"\n").append("\n");
                }
            }

            if (!found) {
                result.append("Занятий в этот день на выбранной неделе нет.\n");
            }
            
        }
        return result.toString();
    }
    
    public static String getTommorowSchedule(int weekNumber, String groupNumber) throws Exception{
        LocalDate tommorow = LocalDate.now().plusDays(1);
        String day = tommorow.getDayOfWeek().toString();
        if("SUNDAY".equals(day)){
            day = "MON";
            if(weekNumber==1){
                weekNumber=2;
            }
            else{
                weekNumber=1;
            }
        }
        day = day.substring(0,3);
        String apiUrl = "https://digital.etu.ru/api/mobile/schedule?weekDay=" + day.toUpperCase() + "&groupNumber=" + groupNumber + "&joinWeeks=false&season="+getSeason()+"&year="+LocalDate.now().getYear();
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Charset", "UTF-8");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject groupSchedule = jsonResponse.getJSONObject(groupNumber);
        JSONObject daysSchedule = groupSchedule.getJSONObject("days");
        JSONObject selectedDay = daysSchedule.optJSONObject(toWeekString(day));
        if (selectedDay == null) {
            return "Нет занятий на этот день.";
        }

        JSONArray lessons = selectedDay.getJSONArray("lessons");
        StringBuilder result = new StringBuilder("Расписание на " + day + ":\n");
        boolean found = false;
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.getJSONObject(i);
            String start_time = lesson.getString("start_time");
            String end_time = lesson.getString("end_time");
            String teacher = lesson.getString("teacher");
            String name = lesson.getString("name");
            String subjectType = lesson.getString("subjectType");
            String room = lesson.getString("room");
            int week = lesson.getInt("week");

            if (week == 0 || week == weekNumber) {
                found = true;
                result.append("("+subjectType+")").append(name).append(" - ").append(teacher).append("\n").append(start_time).append(" - ").append(end_time).append("\n").append("Аудитория: "+room+"\n").append("\n");
            }
        }

        if (!found) {
            result.append("Занятий в этот день на выбранной неделе нет.\n");
        }

        return result.toString();
    }
    
    public static String getNearSchedule(int weekNumber, String groupNumber) throws Exception{
        LocalDate day_L = LocalDate.now();
        String day = day_L.getDayOfWeek().toString();
        if("SUNDAY".equals(day)){
            day = "MON";
            if(weekNumber==1){
                weekNumber=2;
            }
            else{
                weekNumber=1;
            }
        }
        day = day.substring(0,3);
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String temp = currentTime.format(formatter);
        LocalTime nowDate = LocalTime.parse(temp, formatter);

        String apiUrl = "https://digital.etu.ru/api/mobile/schedule?weekDay=" + day.toUpperCase() + "&groupNumber=" + groupNumber + "&joinWeeks=false&season="+getSeason()+"&year="+LocalDate.now().getYear();
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Charset", "UTF-8");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject groupSchedule = jsonResponse.getJSONObject(groupNumber);
        JSONObject daysSchedule = groupSchedule.getJSONObject("days");
        JSONObject selectedDay = daysSchedule.optJSONObject(toWeekString(day));
        if (selectedDay == null) {
            return "Нет занятий на этот день.";
        }
        JSONArray lessons = selectedDay.getJSONArray("lessons");
    
        String nextLesson = null;
        String nextLessonTime = null;
        String nextTeach=null;
        String nextRoom=null;
        String nextLessonEnd = null;
        String nextType = null;
        for (int i = 0; i < lessons.length(); i++) {
            JSONObject lesson = lessons.getJSONObject(i);
            String start_time = lesson.getString("start_time");
            String end_time = lesson.getString("end_time");
            String teacher = lesson.getString("teacher");
            String name = lesson.getString("name");
            String subjectType = lesson.getString("subjectType");
            String room = lesson.getString("room");
            LocalTime start = LocalTime.parse(start_time,formatter);
            if (start.isAfter(nowDate)) {
                if (nextLesson == null || start.isBefore(nowDate)) {
                    nextLesson = name;
                    nextLessonTime = start_time;
                    nextRoom = room;
                    nextTeach = teacher;
                    nextLessonEnd = end_time;
                    nextType = subjectType;
                }
            }
        }

        if (nextLesson == null) {
            return "Занятий на сегодня больше нет.";
        }
        return "Следующее занятие:\n("+nextType+")"+nextLesson+" - "+nextTeach+"\n"+nextLessonTime+" - "+nextLessonEnd+"\nАудитория: "+nextRoom+"\n\n";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        String command = message.getText();

        if (command.startsWith("/DAY")) {
            try {
                String[] parts = command.split(" ");
                if (parts.length != 4) {
                    sendMessage(message.getChatId(), "Неверный формат команды. Используйте: /DAY <день> <неделя> <группа>. Пример: /DAY MON 1 3352");
                    return;
                }

                String day = parts[1].toLowerCase(); 
                int weekNumber = Integer.parseInt(parts[2]);
                String groupNumber = parts[3];

                String schedule = getDaySchedule(day, weekNumber, groupNumber);
                sendMessage(message.getChatId(), schedule);

            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(message.getChatId(), "Произошла ошибка при получении расписания.");
            }
        }
        else if(command.startsWith("/ALL")){
            try {
                String[] parts = command.split(" ");
                if (parts.length != 3) {
                    sendMessage(message.getChatId(), "Неверный формат команды. Используйте: /ALL <неделя> <группа>. Пример: /ALL 1 3352");
                    return;
                }
                int weekNumber = Integer.parseInt(parts[1]);
                String groupNumber = parts[2];

                String schedule = getAllSchedule(weekNumber, groupNumber);
                sendMessage(message.getChatId(), schedule);

            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(message.getChatId(), "Произошла ошибка при получении расписания.");
            }
        }
        else if(command.startsWith("/TOMORROW")){
            try {
                String[] parts = command.split(" ");
                if (parts.length != 3) {
                    sendMessage(message.getChatId(), "Неверный формат команды. Используйте: /TOMORROW <неделя> <группа>. Пример: /TOMORROW 1 3352");
                    return;
                }
                int weekNumber = Integer.parseInt(parts[1]);
                String groupNumber = parts[2];

                String schedule = getTommorowSchedule(weekNumber, groupNumber);
                sendMessage(message.getChatId(), schedule);

            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(message.getChatId(), "Произошла ошибка при получении расписания.");
            }
        
        }
        else if(command.startsWith("/NEAR")){
            try {
                String[] parts = command.split(" ");
                if (parts.length != 3) {
                    sendMessage(message.getChatId(), "Неверный формат команды. Используйте: /NEAR <неделя> <группа>. Пример: /NEAR 1 3352");
                    return;
                }
                int weekNumber = Integer.parseInt(parts[1]);
                String groupNumber = parts[2];

                String schedule = getNearSchedule(weekNumber, groupNumber);
                sendMessage(message.getChatId(), schedule);

            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(message.getChatId(), "Произошла ошибка при получении расписания.");
            }
        
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }



    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new BotSignal());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}