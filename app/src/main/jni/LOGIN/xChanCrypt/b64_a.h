#include "../openssl/rsa.h"
#include "../openssl/pem.h"
#include "../openssl/bio.h"
#include "../openssl/err.h"
#include <iostream>
#include <string>


template <typename T1, typename T2>
using func_result = std::pair<T1, T2>;

namespace crypto {

    using u8string = std::basic_string<uint8_t>;
    using u8string_view = std::basic_string_view<uint8_t>;

    namespace detail {

        struct base64_encoder
        {
            static constexpr size_t encoded_size(size_t n) { return 4 * ((n + 2) / 3); }
            static constexpr size_t decoded_size(size_t n) { return n / 4 * 3; }

            func_result<std::string, size_t> encode(uint8_t* buf, const uint8_t* data, size_t len) const
            {
                BIO* b64 = nullptr;
                BIO* b64_mem = nullptr;

                b64 = BIO_new(BIO_f_base64());
                if (b64 == nullptr) return {"b64 enc: #1", 0};

                b64_mem = BIO_new(BIO_s_mem());
                if (b64_mem == nullptr)
                {
                    BIO_free(b64);
                    return {"b64 enc: #2", 0};
                }

                b64 = BIO_push(b64, b64_mem);

                BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
                BIO_set_close(b64, BIO_CLOSE);
                BIO_set_close(b64_mem, BIO_CLOSE);

                size_t write_sz = 0;
                int32_t ret = 0;
                do
                {
                    ret = BIO_write(b64, data + write_sz, static_cast<int32_t>(len - write_sz));
                    if (ret < 0)
                    {
                        if (BIO_should_retry(b64))
                        {
                            continue;
                        }
                        else
                        {
                            BIO_free_all(b64);
                            return {"b64 enc: #3", 0};
                        }
                    }
                    write_sz += static_cast<size_t>(ret);
                } while (write_sz != len && ret != 0);

                if (write_sz != len)
                {
                    BIO_free_all(b64);
                    return {"b64 enc: #4", 0};
                }

                BIO_flush(b64);

                BUF_MEM* buf_mem = nullptr;
                BIO_get_mem_ptr(b64, &buf_mem);

                memcpy(buf, buf_mem->data, buf_mem->length);

                auto f_result = func_result<std::string, size_t>{{}, buf_mem->length};

                BIO_free_all(b64);
                return f_result;
            }

            func_result<std::string, size_t> decode(uint8_t* buf, const uint8_t* data, size_t len) const
            {
                BIO* b64 = nullptr;
                BIO* b64_mem = nullptr;

                b64 = BIO_new(BIO_f_base64());
                if (b64 == nullptr) return {"b64 dec: #1", 0};

                b64_mem = BIO_new_mem_buf(data, static_cast<int32_t>(len));
                if (b64_mem == nullptr)
                {
                    BIO_free(b64);
                    return {"b64 dec: #2", 0};
                }

                b64 = BIO_push(b64, b64_mem);

                BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
                BIO_set_close(b64, BIO_CLOSE);
                BIO_set_close(b64_mem, BIO_CLOSE);

                size_t read_sz = 0;
                int32_t ret = 0;
                do
                {
                    ret = BIO_read(b64, buf + read_sz, 512);
                    if (ret < 0)
                    {
                        if (BIO_should_retry(b64))
                        {
                            continue;
                        }
                        else
                        {
                            BIO_free_all(b64);
                            return {"b64 dec: #3", 0};
                        }
                    }
                    read_sz += static_cast<size_t>(ret);
                } while (ret != 0);

                if (read_sz == 0)
                {
                    BIO_free_all(b64);
                    return {"b64 dec: #4", 0};
                }

                BIO_free_all(b64);
                return {{}, static_cast<size_t>(read_sz)};
            }
        };
     }

    struct base64
    {
        static crypto::u8string encode(crypto::u8string_view str)
        {
            return encode(str, str.size());
        }

        static crypto::u8string encode(crypto::u8string_view str, size_t len)
        {
            using crypto::detail::base64_encoder;

            crypto::u8string result{};
            base64_encoder b64_enc{};

            result.resize(base64_encoder::encoded_size(len) + 1);

            auto f_result = b64_enc.encode(&result[0], str.data(), len);
            if (f_result.second != 0)
            {
                result.resize(f_result.second);
                return result;
            }
            else
            {
                return crypto::u8string{};
            }
        }

        static crypto::u8string decode(crypto::u8string_view str)
        {
            return decode(str, str.size());
        }

        static crypto::u8string decode(crypto::u8string_view str, size_t len)
        {
            using crypto::detail::base64_encoder;

            crypto::u8string result{};
            base64_encoder b64_enc{};

            result.resize(base64_encoder::decoded_size(len) + 1);

            auto f_result = b64_enc.decode(&result[0], str.data(), len);
            if (f_result.second != 0)
            {
                result.resize(f_result.second);
                return result;
            }
            else
            {
                return crypto::u8string{};
            }
        }
    };
}