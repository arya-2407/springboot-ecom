    package com.ecommerce.project.service;

    import com.ecommerce.project.exceptions.APIException;
    import com.ecommerce.project.exceptions.ResourceNotFoundException;
    import com.ecommerce.project.model.Cart;
    import com.ecommerce.project.model.Category;
    import com.ecommerce.project.model.Product;
    import com.ecommerce.project.payload.CartDTO;
    import com.ecommerce.project.payload.ProductDTO;
    import com.ecommerce.project.payload.ProductResponse;
    import com.ecommerce.project.repositories.CartRepository;
    import com.ecommerce.project.repositories.CategoryRepository;
    import com.ecommerce.project.repositories.ProductRepository;
    import org.modelmapper.ModelMapper;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.util.List;
    import java.util.stream.Collectors;


    @Service
    public class ProductServiceImpl implements ProductService{

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private ModelMapper modelMapper;

        @Autowired
        private FileService fileService;

        @Autowired
        private CartRepository cartRepository;

        @Autowired
        private CartService cartService;

        @Value("${project.image}")
        private String path;

        @Override
        public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));

            boolean isPresent = false;
            List<Product> products = category.getProducts();
            for(Product value : products){
                if (value.getProductName().equals(productDTO.getProductName())) {
                    isPresent = true;
                    break;
                }
            }
            if(!isPresent){
                Product product = modelMapper.map(productDTO, Product.class);
                product.setCategory(category);
                product.setImage("default.png");
                double specialPrice = product.getPrice() -
                        ((product.getDiscount()*0.01) * product.getPrice());
                product.setSpecialPrice(specialPrice);
                Product savedProduct = productRepository.save(product);
                return modelMapper.map(savedProduct,ProductDTO.class);
            }else{
                throw new APIException("Product already exists");
            }

        }

        @Override
        public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

            Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageDetails = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
            Page<Product> productPage = productRepository.findAll(pageDetails);

            List<Product> productList = productPage.getContent();
            if(productList.isEmpty()){
                throw new APIException("No products created");
            }
            List<ProductDTO> productDTOS =  productList.stream()
                    .map(product -> modelMapper.map(product,ProductDTO.class))
                    .toList();
            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOS);
            productResponse.setPageNumber(productPage.getNumber());
            productResponse.setPageSize(productPage.getSize());
            productResponse.setTotalElements(productPage.getTotalElements());
            productResponse.setTotalPages(productPage.getTotalPages());
            productResponse.setLastPage(productPage.isLast());
            return productResponse;
        }

        @Override
        public ProductResponse getProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));

            Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageDetails = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
            Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category,pageDetails);

            List<Product> products = productPage.getContent();
            List<ProductDTO> productDTOS = products.stream()
                    .map(product -> modelMapper.map(product,ProductDTO.class))
                    .toList();
            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOS);
            productResponse.setPageNumber(productPage.getNumber());
            productResponse.setPageSize(productPage.getSize());
            productResponse.setTotalElements(productPage.getTotalElements());
            productResponse.setTotalPages(productPage.getTotalPages());
            productResponse.setLastPage(productPage.isLast());
            return productResponse;
        }

        @Override
        public ProductResponse getProductByName(String productName,Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

            Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageDetails = PageRequest.of(pageNumber,pageSize,sortByAndOrder);
            Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%' + productName + '%',pageDetails);
            List<Product> products = productPage.getContent();
            List<ProductDTO> productDTOS = products.stream()
                    .map(product -> modelMapper.map(product,ProductDTO.class))
                    .toList();
            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOS);
            productResponse.setPageNumber(productPage.getNumber());
            productResponse.setPageSize(productPage.getSize());
            productResponse.setTotalElements(productPage.getTotalElements());
            productResponse.setTotalPages(productPage.getTotalPages());
            productResponse.setLastPage(productPage.isLast());
            return productResponse;
        }

        @Override
        public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
            Product existingProduct = productRepository.findById(productId).
                    orElseThrow(() -> new APIException("Product does not exist"));
            Product product = modelMapper.map(productDTO, Product.class);
            existingProduct.setProductName(product.getProductName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setQuantity(product.getQuantity());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setDiscount(product.getDiscount());
            double specialPrice = product.getPrice() -
                    ((product.getDiscount()*0.01) * product.getPrice());
            existingProduct.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(existingProduct);

            List<Cart> carts = cartRepository.findCartsByProductId(productId);

            List<CartDTO> cartDTOs = carts.stream().map(cart -> {
                CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

                List<ProductDTO> products = cart.getCartItems().stream()
                        .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());

                cartDTO.setProducts(products);

                return cartDTO;

            }).toList();

            cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

            return modelMapper.map(savedProduct,ProductDTO.class);
        }

        @Override
        public ProductDTO deleteProduct(Long productId) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new APIException("Product not found"));
            productRepository.deleteById(productId);
            return modelMapper.map(product,ProductDTO.class);
        }

        @Override
        public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
            Product productFromDb = productRepository.findById(productId)
                                                .orElseThrow(()->new APIException("Product not found"));

            String fileName = fileService.uploadImage(path,image);
            productFromDb.setImage(fileName);
            Product savedProduct = productRepository.save(productFromDb);
            return modelMapper.map(savedProduct,ProductDTO.class);

        }


    }
