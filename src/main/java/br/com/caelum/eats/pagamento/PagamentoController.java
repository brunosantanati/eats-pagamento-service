package br.com.caelum.eats.pagamento;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/pagamentos")
@AllArgsConstructor
class PagamentoController {

	private PagamentoRepository pagamentoRepo;
	private PedidoRestClient pedidoClient;

	@GetMapping("/{id}")
	public Resource<PagamentoDto> detalha(@PathVariable("id") Long id) {
		Pagamento pagamento = pagamentoRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException());
		
		List<Link> links = new ArrayList<>();
		
		Link self = linkTo(methodOn(PagamentoController.class).detalha(id)).withSelfRel();
		links.add(self);
		
		if(Pagamento.Status.CRIADO.equals(pagamento.getStatus())) {
			Link confirma = linkTo(methodOn(PagamentoController.class).confirma(id)).withRel("confirma");
			links.add(confirma);
			
			Link cancela = linkTo(methodOn(PagamentoController.class).cancela(id)).withRel("cancela");
			links.add(cancela);
		}
		
		PagamentoDto dto = new PagamentoDto(pagamento);
		Resource<PagamentoDto> resource = new Resource<PagamentoDto>(dto, links);
		
		return resource;
	}

	@PostMapping
	ResponseEntity<Resource<PagamentoDto>> cria(@RequestBody Pagamento pagamento, UriComponentsBuilder uriBuilder) {
		pagamento.setStatus(Pagamento.Status.CRIADO);
		Pagamento salvo = pagamentoRepo.save(pagamento);
		URI path = uriBuilder.path("/pagamentos/{id}").buildAndExpand(salvo.getId()).toUri();
		PagamentoDto dto = new PagamentoDto(salvo);
		
		Long id = salvo.getId();
		
		List<Link> links = new ArrayList<>();
		
		Link self = linkTo(methodOn(PagamentoController.class).detalha(id)).withSelfRel();
		links.add(self);
		
		Link confirma = linkTo(methodOn(PagamentoController.class).confirma(id)).withRel("confirma");
		links.add(confirma);
		
		Link cancela = linkTo(methodOn(PagamentoController.class).cancela(id)).withRel("cancela");
		links.add(cancela);
		
		Resource<PagamentoDto> resource = new Resource<PagamentoDto>(dto, links);
		
		return ResponseEntity.created(path).body(resource);
	}

	@PutMapping("/{id}")
	public Resource<PagamentoDto> confirma(@PathVariable("id") Long id) {
		Pagamento pagamento = pagamentoRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException());
		pagamento.setStatus(Pagamento.Status.CONFIRMADO);
		pagamentoRepo.save(pagamento);
		
		Long pedidoId = pagamento.getPedidoId();
		pedidoClient.avisaQueFoiPago(pedidoId);
		
		List<Link> links = new ArrayList<>();
		
		Link self = linkTo(methodOn(PagamentoController.class).detalha(id)).withSelfRel();
		links.add(self);
		
		PagamentoDto dto = new PagamentoDto(pagamento);
		Resource<PagamentoDto> resource = new Resource<PagamentoDto>(dto, links);
		
		return resource;
	}

	@DeleteMapping("/{id}")
	public Resource<PagamentoDto> cancela(@PathVariable("id") Long id) {
		Pagamento pagamento = pagamentoRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException());
		pagamento.setStatus(Pagamento.Status.CANCELADO);
		pagamentoRepo.save(pagamento);
		
		List<Link> links = new ArrayList<>();
		
		Link self = linkTo(methodOn(PagamentoController.class).detalha(id)).withSelfRel();
		links.add(self);
		
		PagamentoDto dto = new PagamentoDto(pagamento);
		Resource<PagamentoDto> resource = new Resource<PagamentoDto>(dto, links);
		
		return resource;
	}

}