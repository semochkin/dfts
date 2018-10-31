import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;

import java.io.Serializable;
import java.net.InetAddress;
import java.time.Duration;

/**
 * @author 16779246
 * @since 29.10.18
 */
public class Listener extends AbstractActor
{
	private final class Tick implements Serializable{}

	public final Tick TICK = new Tick();

	LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Cluster cluster = Cluster.get(getContext().getSystem());

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.MemberUp.class);
		ActorRef mediator = DistributedPubSub.get(cluster.system()).mediator();
		mediator.tell(new DistributedPubSubMediator.Subscribe("welcome", getSelf()),
			getSelf());
		mediator.tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
		getContext().getSystem().getScheduler().schedule(Duration.ofSeconds(5), Duration.ofSeconds(3), getSelf(), TICK, getContext().dispatcher(), null);
	}

	//re-subscribe when restart
	@Override
	public void postStop() {
		cluster.unsubscribe(getSelf());
		DistributedPubSub.get(getContext().system()).mediator().tell(new DistributedPubSubMediator.Unsubscribe("welcome", getSelf()), getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(ClusterEvent.MemberUp.class, mUp -> {
				InetAddress local = InetAddress.getLocalHost();
				log.info("Member is Up: {}/:::/{}", mUp.member(), getSender());
				ActorRef mediator = DistributedPubSub.get(getContext().system()).mediator();
				mediator.tell(new DistributedPubSubMediator.Publish("welcome", new CertMsg(local.getCanonicalHostName(), String.valueOf(local.getHostAddress()), "localhost", null)), getSelf());
			})
			.match(ClusterEvent.UnreachableMember.class, mUnreachable ->
				log.info("Member detected as unreachable: {}", mUnreachable.member())
			)
			.match(ClusterEvent.MemberRemoved.class, mRemoved ->
				log.info("Member is Removed: {}", mRemoved.member())
			)
			.match(ClusterEvent.MemberEvent.class, message -> {
				// ignore
			})
			.match(DistributedPubSubMediator.SubscribeAck.class, ack -> {
				ActorRef mediator = DistributedPubSub.get(cluster.system()).mediator();
				mediator.tell(new DistributedPubSubMediator.Publish("welcome", String.valueOf(System.currentTimeMillis())), getSelf());
				mediator.tell(new DistributedPubSubMediator.Send("/user/destination", "34467979",
					false), getSelf());
			})
			.match(Tick.class, ack -> {
				ActorRef mediator = DistributedPubSub.get(cluster.system()).mediator();
				mediator.tell(new DistributedPubSubMediator.Send("/user/destination", String.valueOf(System.currentTimeMillis()) + getSelf().toString(),
					false), getSelf());
			})
			.match(CertMsg.class, message -> {
				log.info("Got message: " + message);
			})
			.match(DistributedPubSubMediator.SubscribeAck.class, msg ->
				log.info("subscribed"))
			.matchAny(obj ->
				log.error("Got " + obj)
			)
			.build();
	}}
