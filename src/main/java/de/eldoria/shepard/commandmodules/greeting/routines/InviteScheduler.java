package de.eldoria.shepard.commandmodules.greeting.routines;

import de.eldoria.shepard.modulebuilder.requirements.ReqDataSource;
import de.eldoria.shepard.modulebuilder.requirements.ReqInit;
import de.eldoria.shepard.modulebuilder.requirements.ReqShardManager;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class InviteScheduler implements ReqShardManager, ReqInit, ReqDataSource {

    private ShardManager shardManager;
    private DataSource source;

    /**
     * Create a new invite scheduler.
     */
    public InviteScheduler() {
    }

    @Override
    public void addShardManager(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public void init() {
        ScheduledExecutorService autoRegister = Executors.newSingleThreadScheduledExecutor();
        autoRegister.schedule(new RegisterInvites(shardManager, source), 0, TimeUnit.SECONDS);

        ScheduledExecutorService refreshInvites = Executors.newSingleThreadScheduledExecutor();
        refreshInvites.scheduleAtFixedRate(new RefreshInvites(shardManager, source), 0, 60, TimeUnit.MINUTES);
    }

    @Override
    public void addDataSource(DataSource source) {
        this.source = source;
    }
}