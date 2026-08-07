import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDruid {

    private static String[] keywords = new String[]{"select", "from", "where"};
    private static Map<String, List<String>> tables = new HashMap<>();

    static {
        tables.put("user", List.of("id", "name", "age"));
        tables.put("student", List.of("id", "name", "age"));
        tables.put("lll", List.of("id", "name", "age"));
    }

    public static void main(String[] args) {
        var sql = "select * from user.";

        var suggestions = new ArrayList<String>();
        var tokens = sql.split("\\s+");

        if (tokens.length == 0) {
            return;
        }
        // Get the last token
        var lastToken = tokens[tokens.length - 1];

        if (lastToken.endsWith(".")) {
            var tableName = lastToken.substring(0, lastToken.length() - 1);
            // Get the fields in the table
            var fields = tables.get(tableName);
            if (fields != null) {
                suggestions.addAll(fields);
            }
        } else {
            // Get the matching keyword from keywords
            for (var keyword : keywords) {
                if (keyword.startsWith(lastToken)) {
                    suggestions.add(keyword);
                }
            }
        }


        System.out.println(suggestions);
    }
}
