package com.pe.feriocuadros.springboot.webflux.handler;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.pe.feriocuadros.springboot.webflux.documents.Categoria;
import com.pe.feriocuadros.springboot.webflux.documents.Producto;
import com.pe.feriocuadros.springboot.webflux.service.ProductService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductHandler {

	@Autowired
	private ProductService service;
	
	@Value("${config.uploads.path}")
	private String path;
	
	@Autowired
	private Validator validator;
	
	public Mono<ServerResponse> upload(ServerRequest request){
		
		String id = request.pathVariable("id");
		return request.multipartData()
				.map(multipart -> multipart.toSingleValueMap().get("file"))
				.cast(FilePart.class)
				.flatMap(file -> service.findById(id)
						.flatMap(p -> {
							p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
							.replace(" ", "")
							.replace(":", "")
							.replace("\\", "")
							.trim());
							
							return file.transferTo(new File(path + p.getFoto()))
									.then(service.save(p));
									
						})).flatMap(p -> ServerResponse
									.created(URI.create("/api/productos/".concat(p.getId())))
									.contentType(MediaType.APPLICATION_JSON)
									.body(fromValue(p)))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> crearConFoto(ServerRequest request){
		
		Mono<Producto> producto = request.multipartData()
				.map(multipart -> {
					FormFieldPart nombre = (FormFieldPart) multipart.toSingleValueMap().get("nombre");
					FormFieldPart precio = (FormFieldPart) multipart.toSingleValueMap().get("precio");
					FormFieldPart categoriaId = (FormFieldPart) multipart.toSingleValueMap().get("categoria.id");
					FormFieldPart categoriaINombre = (FormFieldPart) multipart.toSingleValueMap().get("categoria.nombre");
					
					Categoria categoria = new Categoria(categoriaINombre.value());
					categoria.setId(categoriaId.value());
					return new Producto(nombre.value(), Double.parseDouble(precio.value()), categoria);
				});
		return request.multipartData()
				.map(multipart -> multipart.toSingleValueMap().get("file"))
				.cast(FilePart.class)
				.flatMap(file -> producto
						.flatMap(p -> {
							p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
							.replace(" ", "")
							.replace(":", "")
							.replace("\\", "")
							.trim());
							
							p.setCreateAt(new Date());
							
							return file.transferTo(new File(path + p.getFoto()))
									.then(service.save(p));
									
						})).flatMap(p -> ServerResponse
									.created(URI.create("/api/productos/".concat(p.getId())))
									.contentType(MediaType.APPLICATION_JSON)
									.body(fromValue(p)));
	}
	
	public Mono<ServerResponse> list(ServerRequest request){
		return ServerResponse.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.findAll(), Producto.class);
	}
	
	public Mono<ServerResponse> detail(ServerRequest request){
		String id = request.pathVariable("id");
		return service.findById(id).flatMap(p -> {
			return ServerResponse.ok()
			.contentType(MediaType.APPLICATION_JSON)
			.body(fromValue(p));
		}).switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> crear(ServerRequest request){
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		return producto.flatMap(p -> {
			
			Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
			validator.validate(p, errors);
		
			if (errors.hasErrors()) {
				return Flux.fromIterable(errors.getFieldErrors())
						.map(error -> "El campo " + error.getField() + " " + error.getDefaultMessage())
						.collectList()
						.flatMap(list -> ServerResponse.badRequest().body(fromValue(list)));
			} else {
				if (p.getCreateAt()== null) {
					p.setCreateAt(new Date());
				}
				return service.save(p).flatMap(pdb -> {
					return ServerResponse
							.created(URI.create("/api/productos/".concat(pdb.getId())))
							.contentType(MediaType.APPLICATION_JSON)
							.body(fromValue(pdb));
				});
			}			
		});
	}
	
	public Mono<ServerResponse> editar(ServerRequest request){		
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		Mono<Producto> productoDb = service.findById(request.pathVariable("id"));
		
		return productoDb.zipWith(producto, (db, req) -> {
			db.setNombre(req.getNombre());
			db.setPrecio(req.getPrecio());
			db.setCategoria(req.getCategoria());
			return db;
		}).flatMap(p -> {
			return ServerResponse
					.created(URI.create("/api/productos/".concat(p.getId())))
					.contentType(MediaType.APPLICATION_JSON)
					.body(service.save(p), Producto.class);
		}).switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> eliminar(ServerRequest request){		
		Mono<Producto> producto = service.findById(request.pathVariable("id"));
		
		return producto.flatMap(p -> service.delete(p)
				.then(ServerResponse.noContent().build()))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
}
