import akka.actor.Address;
import akka.cluster.Cluster;

import java.net.*;

import akka.actor.ActorSystem;
import akka.actor.Props;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


public class App
{
	public static void main(String[] args)
	{
		if (args.length == 0)
			startup(new String[]{"2551", "2552", "0"});
		else
			startup(args);
	}

	private static void startup(String[] ports)
	{
		for (String port : ports)
		{
			// Override the configuration of the port
			Config config = ConfigFactory.parseString(
				"akka.remote.netty.tcp.port=" + port + "\n" +
					"akka.remote.artery.canonical.port=" + port)
				.withFallback(ConfigFactory.load());

			// Create an Akka system
			ActorSystem system = ActorSystem.create("ClusterSystem", config);

			// Create an actor that handles cluster domain events
			system.actorOf(Props.create(Listener.class),
				"clusterListener");
			system.actorOf(Props.create(Destination.class), "destination");
		}
	}

}