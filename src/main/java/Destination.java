import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Destination extends AbstractActor
{
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public Destination() {
		ActorRef mediator =
			DistributedPubSub.get(getContext().system()).mediator();
		// register to the path
		mediator.tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(String.class, msg ->
				log.info("Got by topic: {}", msg))
			.build();
	}

}