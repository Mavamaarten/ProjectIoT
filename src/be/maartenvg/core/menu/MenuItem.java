package be.maartenvg.core.menu;

public enum MenuItem {
    SHOW_IP_ADDRESS(0, "Show IP Address"),
    EDIT_COUNTDOWN(1, "Countdown delay"),
    EDIT_COOLDOWN(2, "Cooldown delay"),
    EDIT_BACKLIGHT(3, "Backlight T-out"),
    SHUTDOWN(4, "Shutdown"),
    BACK(5, "Go back (arm)");

    private int value;
    private String menuTitle;

    MenuItem(int value, String menuTitle) {
        this.value = value;
        this.menuTitle = menuTitle;
    }

    public int getValue() {
        return value;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    public static MenuItem findByValue(int value){
        for(MenuItem item : MenuItem.values()){
            if(item.getValue() == value) return item;
        }
        return null;
    }

    public static int count(){
        return MenuItem.values().length;
    }
}
