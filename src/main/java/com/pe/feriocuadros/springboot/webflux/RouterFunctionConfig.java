package com.pe.feriocuadros.springboot.webflux;

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.pe.feriocuadros.springboot.webflux.handler.ProductHandler;

@Configuration
public class RouterFunctionConfig {
	
	@Bean
	public RouterFunction<ServerResponse> routes(ProductHandler handler){
		return route(GET("/api/productos").or(GET("/api/productos/listar")), handler::list)
				.andRoute(GET("/api/productos/{id}"), handler::detail)
				.andRoute(POST("/api/productos"), handler::crear)
				.andRoute(PUT("/api/productos/{id}"), handler::editar)
				.andRoute(DELETE("/api/productos/{id}"), handler::eliminar)
				.andRoute(POST("/api/productos/upload/{id}"), handler::upload)
				.andRoute(POST("/api/productos/crear"), handler::crearConFoto);
	}
	
}
