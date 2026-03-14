#pragma once

#include <utility>
#include <type_traits>

#include <arm_neon.h>

#define XORSTR_F_INLINE __attribute__((always_inline))

#define xorstr(str) jm::xor_string{[]() { return str; }, std::integral_constant<size_t, sizeof(str) / sizeof(*str)>{}, std::make_index_sequence<jm::detail::_buffer_size<sizeof(str)>()>{}}
#define xorstr_(str) xorstr(str).crypt_get()
#define static_xorstr_(str) []() { static const auto decrypted = []() { auto encrypted = xorstr(str); encrypted.crypt(); return encrypted; }(); return decrypted.get(); }()

namespace jm {

    namespace detail {

        template <size_t Size>
        constexpr size_t _buffer_size()
        {
            return ((Size / 16) + (Size % 16 != 0)) * 2;
        }

        template <uint32_t Seed>
        constexpr uint32_t key4() noexcept
        {
            uint32_t value = Seed;
            for(char c : __TIME__)
                value = static_cast<uint32_t>((value ^ c) * 16777619ull);

            return value;
        }

        template <size_t S>
        constexpr uint64_t key8()
        {
            constexpr uint32_t first_part = key4<2166136261 + S>();
            constexpr uint32_t second_part = key4<first_part>();
            return (static_cast<uint64_t>(first_part) << 32) | second_part;
        }

        template <size_t N, class CharT>
        constexpr uint64_t load_xored_str8(uint64_t key, size_t idx, const CharT* str) noexcept
        {
            using cast_type = std::make_unsigned_t<CharT>;

            constexpr size_t value_size = sizeof(CharT);
            constexpr uint64_t idx_offset = 8 / value_size;

            uint64_t value = key;
            for(size_t i = 0; i < idx_offset && i + idx * idx_offset < N; ++i)
                value ^= (uint64_t{static_cast<cast_type>(str[i + idx * idx_offset])} << ((i % idx_offset) * 8 * value_size));

            return value;
        }

        XORSTR_F_INLINE inline uint64_t load_from_reg(uint64_t value) noexcept
        {
            asm("" : "=r"(value) : "0"(value) :);
            return value;
        }

        template <uint64_t V>
        struct uint64_v
        {
            static inline constexpr uint64_t value = V;
        };
    }

    template <class CharT, size_t Size, class Keys, class Indices>
    class xor_string;

    template <class CharT, size_t Size, uint64_t... Keys, size_t... Indices>
    class xor_string<CharT, Size, std::integer_sequence<uint64_t, Keys...>, std::index_sequence<Indices...>>
    {
        using value_type = CharT;
        using const_pointer = const CharT*;
        using size_type = size_t;

    public:
        template <class L>
        XORSTR_F_INLINE xor_string(L l, std::integral_constant<size_t, Size>, std::index_sequence<Indices...>) noexcept
            : _storage{detail::load_from_reg(detail::uint64_v<detail::load_xored_str8<Size>(Keys, Indices, l())>::value)...}
        {
        }

        XORSTR_F_INLINE constexpr size_type size() const noexcept
        {
            return Size - 1;
        }

        XORSTR_F_INLINE void crypt() noexcept
        {
            alignas(alignment) uint64_t arr[] = { detail::load_from_reg(Keys)... };
            uint64_t* keys = reinterpret_cast<uint64_t*>(detail::load_from_reg(reinterpret_cast<uint64_t>(arr)));

            ((Indices >= sizeof(_storage) / 16 ? static_cast<void>(0) :
                __builtin_neon_vst1q_v(
                    reinterpret_cast<uint64_t*>(_storage) + Indices * 2,
                    veorq_u64(
                        __builtin_neon_vld1q_v(_storage + Indices * 2, 51),
                        __builtin_neon_vld1q_v(keys + Indices * 2, 51)
                    ), 51
                )
            ), ...);
        }

        XORSTR_F_INLINE const_pointer get() const noexcept
        {
            return reinterpret_cast<const_pointer>(_storage);
        }

        XORSTR_F_INLINE const_pointer crypt_get() noexcept
        {
            alignas(alignment) uint64_t arr[] = { detail::load_from_reg(Keys)... };
            uint64_t* keys = reinterpret_cast<uint64_t*>(detail::load_from_reg(reinterpret_cast<uint64_t>(arr)));

            ((Indices >= sizeof(_storage) / 16 ? static_cast<void>(0) :
                __builtin_neon_vst1q_v(
                    reinterpret_cast<uint64_t*>(_storage) + Indices * 2,
                    veorq_u64(
                        __builtin_neon_vld1q_v(_storage + Indices * 2, 51),
                        __builtin_neon_vld1q_v(keys + Indices * 2, 51)
                    ), 51
                )
            ), ...);

            return reinterpret_cast<const_pointer>(_storage);
        }
    private:
        static inline constexpr uint64_t alignment = 16;
        alignas(alignment) uint64_t _storage[sizeof...(Keys)] = {};
    };

#if 0
    template <class L, size_t Size, size_t... Indices>
    xor_string(L l, std::integral_constant<size_t, Size>, std::index_sequence<Indices...>)
        -> xor_string<std::remove_cvref_t<decltype(l()[0])>, Size, std::integer_sequence<uint64_t, detail::key8<Indices>()...>, std::index_sequence<Indices...>>;
#else
    template <class L, size_t Size, size_t... Indices>
    xor_string(L l, std::integral_constant<size_t, Size>, std::index_sequence<Indices...>)
        -> xor_string<std::remove_cv_t<std::remove_reference_t<decltype(l()[0])>>, Size, std::integer_sequence<uint64_t, detail::key8<Indices>()...>, std::index_sequence<Indices...>>;
#endif
}