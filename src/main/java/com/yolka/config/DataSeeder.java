package com.yolka.config;

import com.yolka.helpers.UuidGeneratorHelper;
import com.yolka.models.*;
import com.yolka.repositories.TipRepository;
import com.yolka.repositories.VetClinicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

	private final TipRepository tipRepository;
	private final VetClinicRepository vetClinicRepository;
	private final MongoTemplate mongoTemplate;

	@Override
	public void run(String... args) {
		try {
			backfillMissingUuids();
			ensureAllIndexes();

			if (tipRepository.count() == 0) {
				seedTips();
			}
			if (vetClinicRepository.count() == 0) {
				seedVetClinics();
			}
		} catch (Exception e) {
			log.warn("DataSeeder skipped or encountered an error: {}", e.getMessage());
		}
	}

	private void backfillMissingUuids() {
		List<Class<?>> docClasses = List.of(
				UserDocument.class,
				PetDocument.class,
				TipDocument.class,
				VetClinicDocument.class,
				NotificationDocument.class,
				BackupDocument.class
		);
		for (Class<?> clazz : docClasses) {
			try {
				Query missingUuidQuery = new Query(new Criteria().orOperator(
						Criteria.where("uuid").isNull(),
						Criteria.where("uuid").exists(false)
				));
				mongoTemplate.find(missingUuidQuery, clazz).forEach(doc -> {
					mongoTemplate.save(doc);
				});
				// Also ensure any raw document without uuid gets one via direct collection check
				String collectionName = mongoTemplate.getCollectionName(clazz);
				mongoTemplate.getCollection(collectionName)
						.find(new org.bson.Document("$or", List.of(
								new org.bson.Document("uuid", null),
								new org.bson.Document("uuid", new org.bson.Document("$exists", false))
						)))
						.forEach(rawDoc -> {
							Object idObj = rawDoc.get("_id");
							String newUuid = (idObj != null) ? idObj.toString() : UuidGeneratorHelper.newUuid();
							mongoTemplate.getCollection(collectionName).updateOne(
									new org.bson.Document("_id", idObj),
									new org.bson.Document("$set", new org.bson.Document("uuid", newUuid)
											.append("deleted", rawDoc.getBoolean("deleted", false)))
							);
						});
			} catch (Exception e) {
				log.debug("Backfill uuid check for {}: {}", clazz.getSimpleName(), e.getMessage());
			}
		}
	}

	private void ensureAllIndexes() {
		IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoTemplate.getConverter().getMappingContext());
		List<Class<?>> entities = List.of(
				UserDocument.class,
				PetDocument.class,
				TipDocument.class,
				VetClinicDocument.class,
				NotificationDocument.class,
				BackupDocument.class,
				ProcessTokenDocument.class
		);
		for (Class<?> entity : entities) {
			IndexOperations indexOps = mongoTemplate.indexOps(entity);
			resolver.resolveIndexFor(entity).forEach(indexDefinition -> {
				try {
					indexOps.createIndex(indexDefinition);
				} catch (Exception ex) {
					log.debug("Index already exists or skipped on {}: {}", entity.getSimpleName(), ex.getMessage());
				}
			});
		}
		log.info("MongoDB unique and compound indexes verified successfully");
	}

	private void seedTips() {
		List<TipDocument> tips = List.of(
				TipDocument.builder()
						.category("FEEDING")
						.species("ALL")
						.title("Horarios fijos e hidratación constante")
						.summary("Establecer porciones y horarios regulares mejora la digestión de perros y gatos.")
						.content("Dividir la ración diaria en 2 o 3 tomas a la misma hora ayuda al metabolismo de tu mascota y evita la torsión gástrica en razas medianas y grandes. Mantén siempre agua fresca y limpia a su alcance.")
						.build(),
				TipDocument.builder()
						.category("HYGIENE")
						.species("DOG")
						.title("Frecuencia recomendada de baño y corte de uñas")
						.summary("Bañar a tu perro cada 3 o 4 semanas protege los aceites naturales de su piel.")
						.content("Usa siempre champú específico para perros con pH neutro. Revisa sus uñas cada 15 días: si escuchas que chocan contra el piso al caminar, es momento de recortarlas con cuidado sin tocar la vena rosada.")
						.build(),
				TipDocument.builder()
						.category("EXERCISE")
						.species("DOG")
						.title("Paseos diarios y estimulación olfativa")
						.summary("El paseo no solo es ejercicio físico; olfatear reduce el estrés y la ansiedad.")
						.content("Dedica al menos 30 a 45 minutos diarios divididos en mañana y tarde. Permite que tu perro olfatee su entorno y evita sacar a pasear en horas de calor extremo para proteger sus almohadillas del asfalto caliente.")
						.build(),
				TipDocument.builder()
						.category("VACCINATION")
						.species("ALL")
						.title("Importancia del refuerzo anual de vacunación")
						.summary("Mantener al día las vacunas esenciales previene enfermedades graves como parvovirus y rabia.")
						.content("Tanto perros (séxtuple/polivalente y rabia) como gatos (triple felina, leucemia y rabia) requieren refuerzos periódicos. Registra cada aplicación en el expediente clínico de Yolka para recibir recordatorios antes del vencimiento.")
						.build(),
				TipDocument.builder()
						.category("DEWORMING")
						.species("ALL")
						.title("Desparasitación interna y externa")
						.summary("Protege a tu mascota contra parásitos intestinales, pulgas, garrapatas y ácaros.")
						.content("La desparasitación interna suele realizarse cada 3 a 6 meses según el estilo de vida de la mascota, mientras que la externa (pipeta o tableta masticable) se aplica cada 1 a 3 meses.")
						.build(),
				TipDocument.builder()
						.category("GENERAL_CARE")
						.species("CAT")
						.title("Cuidado del arenero y enriquecimiento ambiental")
						.summary("Los gatos necesitan lugares elevados y un arenero limpio alejado de su comida.")
						.content("La regla de oro es tener un arenero por cada gato más uno adicional, limpiándolo diariamente y ubicándolo lejos de sus platos de agua y alimento.")
						.build(),
				TipDocument.builder()
						.category("WELLNESS")
						.species("ALL")
						.title("Señales tempranas para acudir al veterinario")
						.summary("Cambios en el apetito, letargo o consumo excesivo de agua requieren revisión oportuna.")
						.content("Observar las rutinas diarias de tu mascota te permite notar cambios sutiles en su ánimo, pelaje o peso antes de que una condición avance.")
						.build(),
				TipDocument.builder()
						.category("FUN_FACTS")
						.species("ALL")
						.title("Dato curioso: La huella nasal es única")
						.summary("Al igual que las huellas dactilares humanas, la nariz de cada perro y gato tiene un patrón irrepetible.")
						.content("Los pliegues y crestas en la trufa de los perros y gatos forman un patrón único desde su nacimiento. Además, los gatos utilizan sus bigotes como sensores de precisión para medir espacios antes de pasar.")
						.build()
		);
		tipRepository.saveAll(tips);
		log.info("Seeded {} initial care tips in MongoDB", tips.size());
	}

	private void seedVetClinics() {
		List<VetClinicDocument> clinics = List.of(
				VetClinicDocument.builder()
						.name("Hospital Veterinario Yolka Centro")
						.address("Av. Insurgentes Sur 420, Col. Roma Sur")
						.phone("+52 55 5512 3400")
						.latitude(19.4067)
						.longitude(-99.1678)
						.openingHours("Abierto 24 horas")
						.services(List.of("Urgencias 24h", "Vacunación", "Cirugía", "Rayos X"))
						.build(),
				VetClinicDocument.builder()
						.name("Clínica Veterinaria Huellitas Felices")
						.address("Calle Durango 215, Col. Roma Norte")
						.phone("+52 55 5584 9120")
						.latitude(19.4205)
						.longitude(-99.1695)
						.openingHours("Lun - Sáb: 09:00 - 20:00")
						.services(List.of("Consulta General", "Desparasitación", "Estética Canina y Felina"))
						.build(),
				VetClinicDocument.builder()
						.name("Centro Médico Veterinario San Francisco")
						.address("Av. Universidad 1080, Col. Del Valle")
						.phone("+52 55 5604 7711")
						.latitude(19.3679)
						.longitude(-99.1662)
						.openingHours("Lun - Dom: 08:00 - 22:00")
						.services(List.of("Vacunación", "Medicina Preventiva", "Laboratorio Clínico"))
						.build()
		);
		vetClinicRepository.saveAll(clinics);
		log.info("Seeded {} initial veterinary clinics in MongoDB", clinics.size());
	}
}
