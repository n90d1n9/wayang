public class TestJson {
    public static void main(String[] args) {
        String json = "{\"name\": \"list_dir\", \"arguments\": {\"path\": \"/Users/bhangun/Workspace/workkayys/Products/Wayang/wayang-platform/wayang\"}}";
        try {
            tech.kayys.wayang.sdk.json.Json.parse(json);
            System.out.println("Parsed successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
