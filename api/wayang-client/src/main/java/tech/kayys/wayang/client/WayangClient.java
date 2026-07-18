package tech.kayys.wayang.client;

import tech.kayys.wayang.agent.run.WayangRunApi;
import tech.kayys.wayang.agent.run.WayangRunSpecService;
import tech.kayys.wayang.command.WayangCommandApi;
import tech.kayys.wayang.context.WayangContextApi;
import tech.kayys.wayang.contract.WayangContractApi;
import tech.kayys.wayang.skill.WayangSkillApi;

/**
 * Public SDK entry point that groups Wayang's stable product APIs by concern.
 *
 * <p>The lower-level {@link WayangGollekSdk} remains available through
 * {@link #sdk()} for advanced use, while wrappers such as CLI, TUI, HTTP, and
 * future product shells can depend on the smaller sub-APIs exposed here.</p>
 */
public final class WayangClient implements AutoCloseable {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;
    private final WayangCommandApi commands;
    private final WayangContractApi contracts;
    private final WayangSkillApi skills;
    private final WayangProviderApi providers;
    private final WayangPlatformApi platform;
    private final WayangStandardsApi standards;
    private final WayangRunApi runs;
    private final WayangSpecApi specs;
    private final WayangContextApi contexts;

    private WayangClient(WayangGollekSdk sdk) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = new WayangWireApi();
        this.commands = new WayangCommandApi(this.sdk, wire);
        this.contracts = new WayangContractApi(this.sdk, wire);
        this.skills = new WayangSkillApi(this.sdk, wire);
        this.providers = new WayangProviderApi(this.sdk, wire);
        this.platform = new WayangPlatformApi(this.sdk, wire);
        this.standards = new WayangStandardsApi(this.sdk, wire);
        this.runs = new WayangRunApi(this.sdk, wire);
        this.specs = new WayangSpecApi(WayangRunSpecService.create(), this.runs, wire);
        this.contexts = new WayangContextApi(this.sdk, wire);
    }

    public static WayangClient local() {
        return create(WayangGollekSdkConfig.local());
    }

    public static WayangClient remote(String endpoint, String apiKey) {
        return create(WayangGollekSdkConfig.remote(endpoint, apiKey));
    }

    public static WayangClient create(WayangGollekSdkConfig config) {
        return of(Wayang.create(config));
    }

    public static WayangClient of(WayangGollekSdk sdk) {
        return new WayangClient(sdk);
    }

    public WayangGollekSdk sdk() {
        return sdk;
    }

    public WayangWireApi wire() {
        return wire;
    }

    public WayangCommandApi commands() {
        return commands;
    }

    public WayangContractApi contracts() {
        return contracts;
    }

    public WayangSkillApi skills() {
        return skills;
    }

    public WayangProviderApi providers() {
        return providers;
    }

    public WayangPlatformApi platform() {
        return platform;
    }

    public WayangStandardsApi standards() {
        return standards;
    }

    public WayangRunApi runs() {
        return runs;
    }

    public WayangSpecApi specs() {
        return specs;
    }

    public WayangContextApi contexts() {
        return contexts;
    }

    public String productName() {
        return sdk.status().productName();
    }

    @Override
    public void close() {
        sdk.close();
    }
}
