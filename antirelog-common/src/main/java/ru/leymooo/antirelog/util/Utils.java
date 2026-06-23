package ru.leymooo.antirelog.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;

@UtilityClass
public class Utils {
    /**
     * Склоняем слова правильно
     *
     * @param ed неизменяемая часть слова, которую нужно просклонять
     * @param a  окончание для слова, в случае если число оканчивается на 1
     * @param b  окончание для слова, в случае если число оканчивается на 2, 3 или 4
     * @param c  окончание для слова, в случае если число оканчивается на 0, 5...9 и 11...19
     * @param n  число, по которому идёт склонение
     * @return правильно просклонённое слово по числу
     */
    public static String formatTimeUnit(String ed, String a, String b, String c, long n) {
        if (n < 0) {
            n = -n;
        }
        long last = n % 100;
        if (last > 10 && last < 21) {
            return ed + c;
        }
        last = n % 10;
        if (last == 0 || last > 4) {
            return ed + c;
        }
        if (last == 1) {
            return ed + a;
        }
        if (last < 5) {
            return ed + b;
        }
        return ed + c;
    }

    public static String formatTimeUnit(long time) {
        return formatTimeUnit("секунд", "у", "ы", "", time);
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String replaceTime(String message, long time) {
        return message.replace("%time%", Long.toString(time)).replace("%formated-sec%",
                formatTimeUnit("секунд", "у", "ы", "", time));
    }
}
