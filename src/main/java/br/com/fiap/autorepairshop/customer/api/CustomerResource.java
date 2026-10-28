package br.com.fiap.autorepairshop.customer.api;

import br.com.fiap.autorepairshop.customer.application.CustomerService;
import br.com.fiap.autorepairshop.customer.domain.Customer;
import br.com.fiap.autorepairshop.customer.domain.DocumentType;
import br.com.fiap.autorepairshop.shared.api.PageResponse;
import br.com.fiap.autorepairshop.shared.domain.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/admin/customers")
@RolesAllowed("ADMIN")
@Tag(name = "Clientes", description = "Cadastro de clientes identificados por CPF ou CNPJ")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private final CustomerService customers;

    public CustomerResource(CustomerService customers) {
        this.customers = customers;
    }

    @Schema(description = "Dados para cadastro de cliente")
    public record CreateCustomerRequest(
            @Schema(description = "CPF ou CNPJ, com ou sem pontuação; aceita CNPJ alfanumérico",
                    examples = "529.982.247-25") @NotBlank String document,
            @Schema(examples = "Maria Silva") @NotBlank @Size(max = 150) String name,
            @Schema(examples = "maria.silva@email.com") @NotBlank @Email @Size(max = 254) String email,
            @Schema(examples = "11999990000") @Size(max = 30) String phone) {
    }

    @Schema(description = "Dados alteráveis do cliente; o documento não pode ser alterado")
    public record UpdateCustomerRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 30) String phone) {
    }

    @Schema(description = "Cliente")
    public record CustomerResponse(UUID id, String document, DocumentType documentType, String name, String email,
            String phone, Instant createdAt, Instant updatedAt) {

        static CustomerResponse from(Customer customer) {
            return new CustomerResponse(customer.id(), customer.document().number(), customer.document().type(),
                    customer.name(), customer.email(), customer.phone(), customer.createdAt(), customer.updatedAt());
        }
    }

    @POST
    @Operation(summary = "Cadastra um cliente")
    @APIResponse(responseCode = "201", description = "Cliente cadastrado")
    @APIResponse(responseCode = "400", description = "Dados inválidos, inclusive documento inválido")
    @APIResponse(responseCode = "409", description = "Já existe cliente ativo com o documento")
    public Response create(@Valid @NotNull CreateCustomerRequest request, @Context UriInfo uriInfo) {
        Customer customer = customers.register(request.document(), request.name(), request.email(), request.phone());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(customer.id().toString()).build())
                .entity(CustomerResponse.from(customer))
                .build();
    }

    @GET
    @Operation(summary = "Lista clientes ativos",
            description = "Com o parâmetro document, devolve somente o cliente com esse CPF ou CNPJ.")
    public PageResponse<CustomerResponse> list(
            @Parameter(description = "CPF ou CNPJ, com ou sem pontuação") @QueryParam("document") String document,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<Customer> result = document == null || document.isBlank()
                ? customers.list(page, size)
                : customers.findByDocument(document);
        return new PageResponse<>(result.map(CustomerResponse::from).items(), page, size, result.total());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta um cliente")
    @APIResponse(responseCode = "404", description = "Cliente inexistente ou excluído")
    public CustomerResponse get(@PathParam("id") UUID id) {
        return CustomerResponse.from(customers.get(id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Altera nome, e-mail e telefone de um cliente")
    public CustomerResponse update(@PathParam("id") UUID id, @Valid @NotNull UpdateCustomerRequest request) {
        return CustomerResponse.from(customers.update(id, request.name(), request.email(), request.phone()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Exclui logicamente um cliente")
    @APIResponse(responseCode = "204", description = "Cliente excluído")
    public void delete(@PathParam("id") UUID id) {
        customers.delete(id);
    }
}
