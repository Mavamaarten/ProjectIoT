package be.maartenvg.settings;

import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class Settings {
    private static Settings settingsInstance;

    private int countdownDuration, cooldownDuration;
    private String pin;

    private Settings(){}

    public static synchronized Settings getInstance(){
        if(settingsInstance == null){
            settingsInstance = new Settings();
            settingsInstance.load();
        }
        return settingsInstance;
    }

    private void load(){
        try{
            String json = new String(Files.readAllBytes(Paths.get("settings.json")), Charset.forName("UTF-8"));
            JSONObject jsonObject = new JSONObject(json);
            countdownDuration = jsonObject.getInt("countdownDuration");
            cooldownDuration = jsonObject.getInt("cooldownDuration");
            pin = jsonObject.getString("pin");
        } catch(NoSuchFileException e){
            System.out.println("No settings found. Generating default settings.json");
            countdownDuration = 10000;
            cooldownDuration = 2 * 60000;
            pin = "1234";
            save();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("countdownDuration", countdownDuration);
        jsonObject.put("cooldownDuration", cooldownDuration);
        jsonObject.put("pin", pin);

        try {
            String json = jsonObject.toString(4);
            PrintWriter writer = new PrintWriter("settings.json", "UTF-8");
            writer.print(json);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCountdownDuration() {
        return countdownDuration;
    }

    public void setCountdownDuration(int countdownDuration) {
        this.countdownDuration = countdownDuration;
    }

    public int getCooldownDuration() {
        return cooldownDuration;
    }

    public void setCooldownDuration(int cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
