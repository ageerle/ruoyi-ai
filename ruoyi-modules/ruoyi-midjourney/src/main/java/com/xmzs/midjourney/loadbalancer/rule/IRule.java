package com.xmzs.midjourney.loadbalancer.rule;

import com.xmzs.midjourney.loadbalancer.DiscordInstance;

import java.util.List;

public interface IRule {

	DiscordInstance choose(List<DiscordInstance> instances);
}
