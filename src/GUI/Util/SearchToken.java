package GUI.Util;

import Compiler.Scanner.LexerToken;

/**
 * A token for handling searches, wrapping search type and the string.
 * Search tokens are immutable
 * Created by Matt Levine on 3/22/14.
 */
public class SearchToken {

    /** These enumerated search types correspond to the various methods a search can
     *  be performed. They implement double-dispatch to be built into a Visitor scheme.
     */
    public enum SearchTokenType {
        SINGLE{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingle(searchToken,token);}},
        SINGLEREG{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleReg(searchToken,token);}},
        SINGLECS{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleCS(searchToken,token);}},
        SINGLEWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleWW(searchToken,token);}},
        SINGLEREGCS{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleRegCS(searchToken,token);}},
        SINGLEREGWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleRegWW(searchToken,token);}},
        SINGLECSWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleCSWW(searchToken,token);}},
        SINGLEREGCSWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitSingleRegCSWW(searchToken,token);}},

        ALL{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAll(searchToken,token);}},
        ALLREG{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllReg(searchToken,token);}},
        ALLCS{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllCS(searchToken,token);}},
        ALLWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllWW(searchToken,token);}},
        ALLREGCS{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllRegCS(searchToken,token);}},
        ALLREGWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllRegWW(searchToken,token);}},
        ALLCSWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllCSWW(searchToken,token);}},
        ALLREGCSWW{public boolean accept(LexerToken token, SearchToken searchToken){
            return SearchManager.visitAllRegCSWW(searchToken,token);}};
        abstract boolean accept(LexerToken token, SearchToken searchToken);

    }

    /** The type of the Token **/
    public final SearchTokenType type;
    /** The phrase corresponding to the token **/
    public final String phrase;

    /** Creates a new SearchToken
     *
     * @param phrase the phrase to search for
     * @param type the type of the token
     */
    public SearchToken(String phrase, SearchTokenType type){
        this.phrase = phrase;
        this.type = type;
    }

    /** Returns the phrase of the token
     * @return the phrase of the token
     */
    @Override public String toString(){return phrase;}

}
