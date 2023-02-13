package com.laioffer.twitch.external;

import com.laioffer.twitch.external.model.Clip;
import com.laioffer.twitch.external.model.Game;
import com.laioffer.twitch.external.model.Stream;
import com.laioffer.twitch.external.model.TwitchToken;
import com.laioffer.twitch.external.model.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class TwitchService {

    final String twitchClientId;
    final String twitchSecret;
    final TwitchApiClient twitchApiClient;
    final TwitchIdentityClient twitchIdentityClient;

    TwitchToken token = null;

    public TwitchService(
            @Value("${twitch.client-id}") String twitchClientId,
            @Value("${twitch.secret}") String twitchSecret,
            TwitchApiClient twitchApiClient,
            TwitchIdentityClient twitchIdentityClient) {
        this.twitchClientId = twitchClientId;
        this.twitchSecret = twitchSecret;
        this.twitchApiClient = twitchApiClient;
        this.twitchIdentityClient = twitchIdentityClient;
    }

    @Cacheable("top_games")
    public List<Game> getTopGames() {
        return requestWithToken(() ->
                twitchApiClient.getTopGames(bearerToken()).data()
        );
    }

    @Cacheable("games_by_name")
    public List<Game> getGames(String name) {
        return requestWithToken(() ->
                twitchApiClient.getGames(bearerToken(), name).data()
        );
    }

    public List<Stream> getSteams(List<String> gameIds, int first) {
        return requestWithToken(() ->
                twitchApiClient.getStreams(bearerToken(), gameIds, first).data()
        );
    }

    public List<Video> getVideos(String gameId, int first) {
        return requestWithToken(() ->
                twitchApiClient.getVideos(bearerToken(), gameId, first).data()
        );
    }

    public List<Clip> getClips(String gameId, int first) {
        return requestWithToken(() ->
                twitchApiClient.getClips(bearerToken(), gameId, first).data()
        );
    }

    public List<String> getTopGameIds() {
        List<String> topGameIds = new ArrayList<>();
        for (Game game : getTopGames()) {
            topGameIds.add(game.id());
        }
        return topGameIds;
    }

    private <T> T requestWithToken(Supplier<T> requestSupplier) {
        if (token == null) {
            token = twitchIdentityClient.requestAccessToken(twitchClientId, twitchSecret, "client_credentials");
        }
        try {
            return requestSupplier.get();
        } catch (WebClientResponseException.Unauthorized e) {
            token = twitchIdentityClient.requestAccessToken(twitchClientId, twitchSecret, "client_credentials");
            return requestSupplier.get();
        }
    }

    private String bearerToken() {
        return "Bearer " + token.accessToken();
    }
}
