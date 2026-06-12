package tech.kayys.wayang.a2ui.wayang.session;

/**
 * Composition helpers for common A2UI session config source registries.
 */
public final class SessionConfigSourceRegistries {

    public static SessionConfigSourceRegistry database(SessionConfigLookupProvider.LookupReader reader) {
        return standardBuilderWithDatabase(reader).build();
    }

    public static SessionConfigSourceRegistry configService(SessionConfigLookupProvider.LookupReader reader) {
        return standardBuilderWithConfigService(reader).build();
    }

    public static SessionConfigSourceRegistry s3(SessionConfigObjectStorageProvider.ObjectReader reader) {
        return standardBuilderWithS3(reader).build();
    }

    public static SessionConfigSourceRegistry rustfs(SessionConfigObjectStorageProvider.ObjectReader reader) {
        return standardBuilderWithRustfs(reader).build();
    }

    public static SessionConfigSourceRegistry objectStorage(
            SessionConfigObjectStorageProvider.ObjectReader s3Reader,
            SessionConfigObjectStorageProvider.ObjectReader rustfsReader) {
        return standardBuilderWithObjectStorage(s3Reader, rustfsReader).build();
    }

    public static SessionConfigSourceRegistry.Builder standardBuilderWithDatabase(
            SessionConfigLookupProvider.LookupReader reader) {
        return registerDatabase(SessionConfigSourceRegistry.standardBuilder(), reader);
    }

    public static SessionConfigSourceRegistry.Builder standardBuilderWithConfigService(
            SessionConfigLookupProvider.LookupReader reader) {
        return registerConfigService(SessionConfigSourceRegistry.standardBuilder(), reader);
    }

    public static SessionConfigSourceRegistry.Builder standardBuilderWithS3(
            SessionConfigObjectStorageProvider.ObjectReader reader) {
        return registerS3(SessionConfigSourceRegistry.standardBuilder(), reader);
    }

    public static SessionConfigSourceRegistry.Builder standardBuilderWithRustfs(
            SessionConfigObjectStorageProvider.ObjectReader reader) {
        return registerRustfs(SessionConfigSourceRegistry.standardBuilder(), reader);
    }

    public static SessionConfigSourceRegistry.Builder standardBuilderWithObjectStorage(
            SessionConfigObjectStorageProvider.ObjectReader s3Reader,
            SessionConfigObjectStorageProvider.ObjectReader rustfsReader) {
        return registerRustfs(registerS3(SessionConfigSourceRegistry.standardBuilder(), s3Reader), rustfsReader);
    }

    public static SessionConfigSourceRegistry.Builder registerDatabase(
            SessionConfigSourceRegistry.Builder builder,
            SessionConfigLookupProvider.LookupReader reader) {
        return resolve(builder).register(
                SessionConfigLookupProvider.TYPE_DATABASE,
                SessionConfigLookupProvider.database(reader));
    }

    public static SessionConfigSourceRegistry.Builder registerConfigService(
            SessionConfigSourceRegistry.Builder builder,
            SessionConfigLookupProvider.LookupReader reader) {
        return resolve(builder).register(
                SessionConfigLookupProvider.TYPE_CONFIG_SERVICE,
                SessionConfigLookupProvider.configService(reader));
    }

    public static SessionConfigSourceRegistry.Builder registerS3(
            SessionConfigSourceRegistry.Builder builder,
            SessionConfigObjectStorageProvider.ObjectReader reader) {
        return resolve(builder).register(
                SessionConfigObjectStorageProvider.TYPE_S3,
                SessionConfigObjectStorageProvider.s3(reader));
    }

    public static SessionConfigSourceRegistry.Builder registerRustfs(
            SessionConfigSourceRegistry.Builder builder,
            SessionConfigObjectStorageProvider.ObjectReader reader) {
        return resolve(builder).register(
                SessionConfigObjectStorageProvider.TYPE_RUSTFS,
                SessionConfigObjectStorageProvider.rustfs(reader));
    }

    private static SessionConfigSourceRegistry.Builder resolve(SessionConfigSourceRegistry.Builder builder) {
        return builder == null ? SessionConfigSourceRegistry.standardBuilder() : builder;
    }

    private SessionConfigSourceRegistries() {
    }
}
