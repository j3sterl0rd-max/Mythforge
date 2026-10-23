package dev.mythforge.server;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Custom cosmetics must not masquerade as server-assigned roles. */
final class PrefixPolicy {
    private static final Pattern COLORS=Pattern.compile("(?i)[&§][0-9a-fk-orx]");
    private static final Set<String> RESERVED=new HashSet<>(Set.of(
        "moderator","administrator","admin","owner","coowner","staff","helper",
        "developer","founder","operator","gamemaster","junior_mod","jrmod","srmod",
        "headmod","trialmod","seniormod","juniormod","support","official",
        "ember","artisan","runeforged","mythic","beta tester","betatester",
        "prestige","donor","donator","supporter","vip","mvp"));
    static void reserve(String value){String key=compact(value);if(!key.isBlank())RESERVED.add(key);}
    static String normalize(String text){
        String value=Normalizer.normalize(COLORS.matcher(text).replaceAll(""),Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        StringBuilder out=new StringBuilder();
        for(int cp:value.codePoints().toArray()){
            if(Character.getType(cp)==Character.NON_SPACING_MARK||Character.getType(cp)==Character.FORMAT)continue;
            // Common Latin-looking Greek/Cyrillic substitutions and leetspeak.
            cp=switch(cp){
                case '0',0x043e,0x03bf->'o';case '1','!', '|',0x0456,0x03b9->'i';
                case '3',0x0435,0x03b5->'e';case '4','@',0x0430,0x03b1->'a';
                case '5','$',0x0455->'s';case '7',0x0442,0x03c4->'t';case '8',0x0432->'b';
                case 0x0440,0x03c1->'p';case 0x0441->'c';case 0x0445,0x03c7->'x';
                case 0x0443->'y';case 0x043c->'m';case 0x043d->'h';case 0x043a->'k';
                case 0x0501->'d';case 0x051b->'q';default->cp;
            };
            out.appendCodePoint(cp>='a'&&cp<='z'?cp:' ');
        }
        return out.toString();
    }
    static String compact(String text){return normalize(text).replace(" ","");}
    static boolean allowed(String text){
        String normalized=normalize(text),key=normalized.replace(" ","");
        if(key.isBlank())return text.isBlank();
        for(String reserved:RESERVED)if(key.contains(compact(reserved)))return false;
        // Short abbreviations are words, so ordinary prefixes such as Hope stay usable.
        for(String word:normalized.split(" +"))if(Set.of("op","mod","gm","dev").contains(word))return false;
        return !Set.of("op","mod","gm","dev").contains(key);
    }
    static String displayed(String saved){return allowed(saved)?saved:"";}
}
