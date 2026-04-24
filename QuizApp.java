import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizApp {

    // 🔴 PUT YOUR REAL REG NUMBER HERE
    static String REG_NO = "RA2311026020156";

    static String BASE_URL =
            "https://devapigw.vidalhealthtpa.com/srm-quiz-task";

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        Set<String> uniqueEvents = new HashSet<>();
        Map<String, Integer> scoreMap = new HashMap<>();

        // Poll API 10 times
        for (int i = 0; i < 10; i++) {

            String url = BASE_URL +
                    "/quiz/messages?regNo=" +
                    REG_NO + "&poll=" + i;

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .build();

            HttpResponse<String> response =
                    client.send(request,
                            HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            System.out.println("Poll " + i + " Response:");
            System.out.println(json);

            processEvents(json, uniqueEvents, scoreMap);

            if (i < 9) {
                Thread.sleep(5000); // 5 sec delay
            }
        }

        // Create leaderboard
        List<Map.Entry<String, Integer>> list =
                new ArrayList<>(scoreMap.entrySet());

        list.sort((a, b) -> b.getValue() - a.getValue());

        String leaderboardJson = buildLeaderboardJson(list);

        System.out.println("\nFinal Leaderboard:");
        System.out.println(leaderboardJson);

        // Submit once
        submitLeaderboard(client, leaderboardJson);
    }

    static void processEvents(String json,
                              Set<String> uniqueEvents,
                              Map<String, Integer> scoreMap) {

        Pattern pattern = Pattern.compile(
                "\\{\"roundId\":\"(.*?)\",\"participant\":\"(.*?)\",\"score\":(\\d+)"
        );

        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {

            String roundId = matcher.group(1);
            String participant = matcher.group(2);
            int score = Integer.parseInt(matcher.group(3));

            String key = roundId + "_" + participant;

            // Deduplicate
            if (!uniqueEvents.contains(key)) {

                uniqueEvents.add(key);

                scoreMap.put(
                        participant,
                        scoreMap.getOrDefault(participant, 0) + score
                );
            }
        }
    }

    static String buildLeaderboardJson(
            List<Map.Entry<String, Integer>> list) {

        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("\"regNo\":\"").append(REG_NO).append("\",");
        sb.append("\"leaderboard\":[");

        for (int i = 0; i < list.size(); i++) {

            Map.Entry<String, Integer> e = list.get(i);

            sb.append("{");
            sb.append("\"participant\":\"")
                    .append(e.getKey()).append("\",");
            sb.append("\"totalScore\":")
                    .append(e.getValue());
            sb.append("}");

            if (i < list.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]}");

        return sb.toString();
    }

    static void submitLeaderboard(HttpClient client,
                                  String body) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                BASE_URL + "/quiz/submit"))
                        .header("Content-Type",
                                "application/json")
                        .POST(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();

        HttpResponse<String> response =
                client.send(request,
                        HttpResponse.BodyHandlers.ofString());

        System.out.println("\nSubmission Response:");
        System.out.println(response.body());
    }
}