package tech.kayys.wayang.gollek.sdk.remote;

record RemoteResponse(int statusCode, String body) {

    RemoteResponse {
        body = body == null ? "" : body;
    }
}
