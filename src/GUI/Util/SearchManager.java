package GUI.Util;

import Compiler.Scanner.LexerToken;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class manages search functions. It makes use of the Visitor Pattern, though is
 * not named as such to avoid confusion with the semantic analysis Visitors. It is,
 * however, a "Visitor" in the sense that it uses double dispatch against enumerated
 * types in order to make the code tremendously more elegant.
 * Created by Matt Levine on 3/23/14.
 */
public class SearchManager {

    public static LexerToken lastSelected = null;
    public static LexerToken selectionYield = null;


    /** Returns true if the token was searched for. Could be made much more
     * elagant by using a functionMapProxy.
     * @param token the token to verify
     * @param searchToken the token to verify against
     * @return true if searched for
     */
    public static boolean isSearchedFor(LexerToken token, SearchToken searchToken){
        return !(searchToken == null || searchToken.phrase.equals("")) && searchToken.type.accept(token, searchToken);
    }

    /** Subroutine for making sure single searches only execute once, and at the right place
     * @param lexerToken the lexer token
     * @param flag subroutine flag (whether matches globally)
     * @return true if matches
     */
    private static boolean findSingleSubroutine(LexerToken lexerToken, boolean flag){
        if(lastSelected == null && flag) {
            lastSelected = lexerToken;
            selectionYield = lexerToken;
            return true;
        }
        else{
            if(lastSelected != null && lastSelected.getLineNum() == lexerToken.getLineNum() &&
                    lastSelected.getColNum() == lexerToken.getColNum()){
                lastSelected = null;
                return false;
            }
        }
        return false;
    }

    /** Returns true if the search token matches the lexer token of single type
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingle(SearchToken searchToken, LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAll(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token of single regex type
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleReg(SearchToken searchToken, LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllReg(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token of single type
     * case sensitivelu
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleCS(SearchToken searchToken, LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllCS(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token of single type
     * only for whole word matches
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleWW(SearchToken searchToken, LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllWW(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token of single type
     * case sensitively only for whole word matches
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleCSWW(SearchToken searchToken,
                                          LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllCSWW(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token of single regex type
     * case sensitively
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleRegCS(SearchToken searchToken,
                                           LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllRegCS(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token of single regex type
     * whole word only
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleRegWW(SearchToken searchToken, LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllRegWW(searchToken,
                lexerToken));
    }

    /** Returns true if the search token matches the lexer token regex type
     * whole word only case sensitively
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitSingleRegCSWW(SearchToken searchToken, LexerToken lexerToken){
        return findSingleSubroutine(lexerToken,visitAllRegCSWW(
                searchToken,lexerToken));
    }

    /** Returns true if the search token matches the lexer token
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAll(SearchToken searchToken, LexerToken lexerToken){
        return lexerToken.getValue().toUpperCase().contains(searchToken.phrase.toUpperCase());
    }

    /** Returns true if the search token matches the lexer token of regex type
     * whole word only
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllReg(SearchToken searchToken, LexerToken lexerToken){
        return regexSubroutine(".*" + searchToken.phrase.toLowerCase() + ".*",
                lexerToken.getValue().toLowerCase());
    }

    /** Returns true if the search token matches the lexer token case sensitively
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllCS(SearchToken searchToken, LexerToken lexerToken){
        return lexerToken.getValue().contains(searchToken.phrase);
    }

    /** Returns true if the search token matches the lexer token of a
     * whole word only
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllWW(SearchToken searchToken, LexerToken lexerToken){
        return lexerToken.getValue().toUpperCase().equals(
                searchToken.phrase.toUpperCase());
    }

    /** Returns true if the search token matches the lexer token of type
     * whole word only, case sensitively
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllCSWW(SearchToken searchToken, LexerToken lexerToken){
        return lexerToken.getValue().equals(searchToken.phrase);
    }

    /** Returns true if the search token matches the lexer token of regex type
     * case sensitively, which is undefined and therefore always false
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllRegCS(SearchToken searchToken, LexerToken lexerToken){
        return regexSubroutine(".*"+searchToken.phrase+".*",lexerToken.getValue());
    }
    /** Returns true if the search token matches the lexer token of regex type
     * whole word only, which is undefined and therefore always false
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllRegWW(SearchToken searchToken, LexerToken lexerToken){
        return regexSubroutine(searchToken.phrase.toLowerCase(),
                lexerToken.getValue().toLowerCase());
    }
    /** Returns true if the search token matches the lexer token of regex type
     * case sensitively, whole word only, which is undefined and therefore always false
     * @param searchToken the token to verify against
     * @param lexerToken the lexer token
     * @return true if matched
     */
    public static boolean visitAllRegCSWW(SearchToken searchToken,
                                          LexerToken lexerToken){
        return regexSubroutine(searchToken.phrase,lexerToken.getValue());
    }

    /** Subroutine for searching for regular expressions
     * @param pattern the regex
     * @param token the phrase
     * @return if matched
     */
    private static boolean regexSubroutine(String pattern, String token){
        try{
            return (Pattern.compile(pattern).matcher(token).matches());
        }catch(PatternSyntaxException e) {return false;}

    }

}
